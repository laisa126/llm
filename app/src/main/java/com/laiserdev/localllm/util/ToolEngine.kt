package com.laiserdev.localllm.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.laiserdev.localllm.data.model.ToolCall
import com.laiserdev.localllm.data.model.ToolResult
import com.laiserdev.localllm.data.repository.LLMRepository
import com.laiserdev.localllm.data.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class ToolEngine(
    private val context: Context,
    private val llm: LLMRepository,
    private val terminal: TerminalExecutor,
    private val pkgManager: PackageManager,
    private val projects: ProjectRepository
) {
    companion object {
        val TOOL_DEFINITIONS = """
Available tools (call them using <tool_call name="TOOL_NAME">JSON_ARGS</tool_call>):

1.  read_file       {"path": "absolute or relative path"}
2.  write_file      {"path": "...", "content": "full file content"}
3.  create_file     {"path": "...", "content": ""}
4.  delete_file     {"path": "..."}
5.  list_files      {"path": "directory path", "recursive": false}
6.  make_dir        {"path": "..."}
7.  apply_diff      {"path": "...", "old": "text to replace", "new": "replacement"}
8.  grep_files      {"path": "dir", "pattern": "regex", "recursive": true}
9.  get_file_info   {"path": "..."}
10. run_command     {"command": "shell command", "cwd": "optional working dir"}
11. install_package {"manager": "npm|pip|apt", "packages": ["pkg1","pkg2"]}
12. search_web      {"query": "search query"}
13. http_get        {"url": "https://..."}
14. list_processes  {"filter": "optional name filter"}
15. kill_process    {"pid": 1234}
16. download_zip    {"url": "https://...", "dest_dir": "relative or absolute path"}
17. screenshot      {"path": "optional output path (default: screenshots/capture.png)"}

Rules:
- Always read a file before editing it
- After writing code, run it to verify
- If a package is missing, install it first
- End with <final_answer>YOUR RESPONSE</final_answer> when done
""".trimIndent()

        // Context window by model format:
        // .litertlm = Gemma 4 = 32K tokens → use ~28K chars of history
        // .task      = Gemma 3/other = 4K tokens → use 3K chars of history
        private fun maxHistoryChars(modelId: String?): Int =
            if (modelId?.contains("gemma4") == true || modelId?.contains("e2b") == true || modelId?.contains("e4b") == true)
                28_000 else 3_000
    }

    // ── Agentic loop ──────────────────────────────────────────────────────────
    fun agentLoop(
        userPrompt: String,
        projectPath: String,
        systemExtra: String = "",
        maxIterations: Int = 10
    ): Flow<AgentEvent> = channelFlow {
        terminal.workingDir = File(projectPath)

        val currentModelId = llm.currentModel()
        val maxHistChars   = maxHistoryChars(currentModelId)

        val system = buildString {
            appendLine("You are an expert AI coding assistant with full access to the file system.")
            appendLine("Project path: $projectPath")
            if (systemExtra.isNotBlank()) appendLine(systemExtra)
            appendLine()
            appendLine(TOOL_DEFINITIONS)
        }

        val history = mutableListOf<Pair<String, String>>() // role → content
        var currentPrompt = userPrompt
        var iterations = 0
        var lastAiText = ""  // FIX 4: track last AI response for fallback

        send(AgentEvent.Thinking("Analyzing task..."))

        while (iterations < maxIterations) {
            iterations++

            val fullPrompt = buildConversationPrompt(history, currentPrompt, maxHistChars)
            val responseBuilder = StringBuilder()
            send(AgentEvent.Thinking("Thinking (step $iterations)..."))

            llm.generateStream(fullPrompt, system).collect { token ->
                responseBuilder.append(token)
            }

            val response = responseBuilder.toString()
            lastAiText = response
            history.add("user" to currentPrompt)
            history.add("assistant" to response)

            // FIX 1: parse tool calls with regex fallback
            val toolCalls = parseToolCallsRobust(response)

            // Check for <final_answer> tag first
            val finalMatch = Regex("<final_answer>([\\s\\S]*?)</final_answer>").find(response)
            if (finalMatch != null) {
                val clean = finalMatch.groupValues[1].trim()
                send(AgentEvent.FinalAnswer(clean))
                return@channelFlow
            }

            if (toolCalls.isEmpty()) {
                // No tool calls, no final_answer tag → treat whole response as final answer
                val clean = response
                    .replace(Regex("<tool_call[^>]*>[\\s\\S]*?</tool_call>"), "")
                    .trim()
                send(AgentEvent.FinalAnswer(clean.ifBlank { "Task complete." }))
                return@channelFlow
            }

            val toolResults = StringBuilder()
            for (tc in toolCalls) {
                send(AgentEvent.ToolCalling(tc.name, tc.args))

                // FIX 3: retry on tool error (max 2 retries)
                var result = executeTool(tc, projectPath)
                var retries = 0
                while (result.isError && retries < 2) {
                    retries++
                    send(AgentEvent.Thinking("Tool error, retrying ($retries/2)..."))
                    result = executeTool(tc, projectPath)
                }

                send(AgentEvent.ToolResult(tc.name, result.output, result.isError))
                toolResults.appendLine("<tool_result name=\"${tc.name}\">")
                toolResults.appendLine(result.output)
                toolResults.appendLine("</tool_result>")
            }

            currentPrompt = toolResults.toString().trim()
        }

        // FIX 4: FinalAnswer fallback — emit last AI text instead of just an error
        val fallback = lastAiText
            .replace(Regex("<tool_call[^>]*>[\\s\\S]*?</tool_call>"), "")
            .trim()
        if (fallback.isNotBlank()) {
            send(AgentEvent.FinalAnswer(fallback))
        } else {
            send(AgentEvent.Error("Max iterations reached. Task may be incomplete."))
        }
    }

    // ── Tool execution ────────────────────────────────────────────────────────
    private suspend fun executeTool(tc: ToolCall, projectPath: String): ToolResult {
        return try {
            // FIX 1: robust arg parsing — try JSON first, fall back to regex extraction
            val args = parseArgsRobust(tc.args)

            when (tc.name) {
                "read_file" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] read_file requires 'path'", true)
                    val f = File(path)
                    if (!f.exists()) return ToolResult("[ERR] File not found: $path", true)
                    ToolResult("```\n${f.readText()}\n```")
                }
                "write_file" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] write_file requires 'path'", true)
                    val content = args["content"]
                        ?: return ToolResult("[ERR] write_file requires 'content'", true)
                    File(path).also { it.parentFile?.mkdirs() }.writeText(content)
                    ToolResult("[OK] Written: $path (${content.length} chars)")
                }
                "create_file" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] create_file requires 'path'", true)
                    val content = args["content"] ?: ""
                    File(path).also { it.parentFile?.mkdirs() }.writeText(content)
                    ToolResult("[OK] Created: $path")
                }
                "delete_file" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] delete_file requires 'path'", true)
                    val f = File(path)
                    val deleted = if (f.isDirectory) f.deleteRecursively() else f.delete()
                    ToolResult(if (deleted) "[OK] Deleted: $path" else "[ERR] Could not delete: $path", !deleted)
                }
                "list_files" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] list_files requires 'path'", true)
                    val recursive = args["recursive"]?.equals("true", ignoreCase = true) ?: false
                    val files = if (recursive) {
                        File(path).walkTopDown().map {
                            "${if (it.isDirectory) "[DIR]" else "[FILE]"} ${it.relativeTo(File(path))}"
                        }.joinToString("\n")
                    } else {
                        File(path).listFiles()?.joinToString("\n") {
                            "${if (it.isDirectory) "[DIR]" else "[FILE]"} ${it.name}"
                        } ?: "Empty directory"
                    }
                    ToolResult(files)
                }
                "make_dir" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] make_dir requires 'path'", true)
                    File(path).mkdirs()
                    ToolResult("[OK] Directory created: $path")
                }
                "apply_diff" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] apply_diff requires 'path'", true)
                    val old = args["old"] ?: return ToolResult("[ERR] apply_diff requires 'old'", true)
                    val new = args["new"] ?: return ToolResult("[ERR] apply_diff requires 'new'", true)
                    val f = File(path)
                    val original = f.readText()
                    if (!original.contains(old)) return ToolResult("[ERR] Pattern not found in file", true)
                    f.writeText(original.replace(old, new))
                    ToolResult("[OK] Diff applied to $path")
                }
                "grep_files" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] grep_files requires 'path'", true)
                    val pattern = args["pattern"] ?: return ToolResult("[ERR] grep_files requires 'pattern'", true)
                    val recursive = args["recursive"]?.equals("true", ignoreCase = true) ?: true
                    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                    val results = mutableListOf<String>()
                    val root = File(path)
                    val files = if (recursive) root.walkTopDown().filter { it.isFile }
                    else root.listFiles()?.asSequence()?.filter { it.isFile } ?: emptySequence()
                    files.forEach { f ->
                        f.readLines().forEachIndexed { i, line ->
                            if (regex.containsMatchIn(line))
                                results.add("${f.relativeTo(root)}:${i + 1}: $line")
                        }
                    }
                    ToolResult(if (results.isEmpty()) "No matches found" else results.joinToString("\n"))
                }
                "get_file_info" -> {
                    val path = resolveArg(args, "path", projectPath)
                        ?: return ToolResult("[ERR] get_file_info requires 'path'", true)
                    val f = File(path)
                    ToolResult("""
                        path: ${f.absolutePath}
                        exists: ${f.exists()}
                        type: ${if (f.isDirectory) "directory" else "file"}
                        size: ${f.length()} bytes
                        lastModified: ${java.util.Date(f.lastModified())}
                        canRead: ${f.canRead()}
                        canWrite: ${f.canWrite()}
                    """.trimIndent())
                }
                "run_command" -> {
                    val command = args["command"] ?: return ToolResult("[ERR] run_command requires 'command'", true)
                    val cwd = args["cwd"]
                    if (cwd != null) terminal.workingDir = File(resolvePath(cwd, projectPath))
                    val output = StringBuilder()
                    terminal.execute(command).collect { (line, isErr) ->
                        output.appendLine(if (isErr) "stderr: $line" else line)
                    }
                    ToolResult(output.toString().trim())
                }
                "install_package" -> {
                    val manager = args["manager"] ?: return ToolResult("[ERR] install_package requires 'manager'", true)
                    val pkgsRaw = args["packages"] ?: return ToolResult("[ERR] install_package requires 'packages'", true)
                    val packages = pkgsRaw.trim('[', ']').split(",").map { it.trim().trim('"') }
                    val result = pkgManager.install(manager, packages, projectPath)
                    ToolResult(result)
                }
                "search_web" -> {
                    val query = args["query"] ?: return ToolResult("[ERR] search_web requires 'query'", true)
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        URL("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1").readText()
                    }
                    val json = Json.parseToJsonElement(response).jsonObject
                    val answer = json["AbstractText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        ?: json["Answer"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        ?: "No direct answer found."
                    ToolResult(answer)
                }
                "http_get" -> {
                    val url = args["url"] ?: return ToolResult("[ERR] http_get requires 'url'", true)
                    val content = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        URL(url).readText().take(4000)
                    }
                    ToolResult(content)
                }
                "list_processes" -> {
                    val filter = args["filter"] ?: ""
                    val output = StringBuilder()
                    terminal.execute("ps aux").collect { (line, _) ->
                        if (filter.isEmpty() || line.contains(filter, ignoreCase = true))
                            output.appendLine(line)
                    }
                    ToolResult(output.toString())
                }
                "kill_process" -> {
                    val pid = args["pid"] ?: return ToolResult("[ERR] kill_process requires 'pid'", true)
                    val output = StringBuilder()
                    terminal.execute("kill $pid").collect { (line, _) -> output.appendLine(line) }
                    ToolResult(output.toString().ifBlank { "[OK] Killed PID $pid" })
                }
                "download_zip" -> {
                    val url = args["url"] ?: return ToolResult("[ERR] download_zip requires 'url'", true)
                    val destDir = resolvePath(args["dest_dir"] ?: "downloads", projectPath)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        File(destDir).mkdirs()
                        val conn = URL(url).openConnection() as HttpURLConnection
                        conn.apply {
                            instanceFollowRedirects = true
                            connectTimeout = 15_000
                            readTimeout = 60_000
                            setRequestProperty("User-Agent", "LocalLLMAgent/1.0")
                        }
                        conn.connect()
                        if (conn.responseCode !in 200..299)
                            return@withContext ToolResult("[ERR] HTTP ${conn.responseCode}", true)
                        var extracted = 0
                        val extractedFiles = mutableListOf<String>()
                        conn.inputStream.use { raw ->
                            ZipInputStream(raw.buffered(65536)).use { zip ->
                                var entry = zip.nextEntry
                                while (entry != null) {
                                    val outFile = File(destDir, entry.name)
                                    if (entry.isDirectory) outFile.mkdirs()
                                    else {
                                        outFile.parentFile?.mkdirs()
                                        FileOutputStream(outFile).use { zip.copyTo(it) }
                                        extractedFiles.add(entry.name)
                                        extracted++
                                    }
                                    zip.closeEntry()
                                    entry = zip.nextEntry
                                }
                            }
                        }
                        ToolResult(buildString {
                            appendLine("[OK] Extracted $extracted files to $destDir")
                            extractedFiles.take(20).forEach { appendLine("  $it") }
                            if (extractedFiles.size > 20) appendLine("  ...and ${extractedFiles.size - 20} more")
                        })
                    }
                }
                "screenshot" -> {
                    val relPath = args["path"] ?: "screenshots/capture.png"
                    val outputPath = resolvePath(relPath, projectPath)
                    var result: ToolResult? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    Handler(Looper.getMainLooper()).post {
                        result = try {
                            val r = com.laiserdev.localllm.util.WebViewRegistry.captureToFile(outputPath)
                            if (r.isSuccess) ToolResult("[OK] Screenshot saved to ${r.getOrNull()}")
                            else ToolResult("[ERR] ${r.exceptionOrNull()?.message}", true)
                        } catch (e: Exception) {
                            ToolResult("[ERR] ${e.message}", true)
                        }
                        latch.countDown()
                    }
                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    result ?: ToolResult("[ERR] Screenshot timed out", true)
                }
                else -> ToolResult("[ERR] Unknown tool: ${tc.name}", true)
            }
        } catch (e: Exception) {
            ToolResult("[ERR] Tool execution failed: ${e.message?.take(200)}", true)
        }
    }

    // ── FIX 1: Robust tool call parser ────────────────────────────────────────
    // Primary: XML tag regex. Fallback: extract name + args even from malformed output.
    private fun parseToolCallsRobust(text: String): List<ToolCall> {
        // Primary — well-formed tags
        val primary = Regex("""<tool_call name="([^"]+)">([\s\S]*?)</tool_call>""")
            .findAll(text).map { ToolCall(it.groupValues[1], it.groupValues[2].trim()) }.toList()
        if (primary.isNotEmpty()) return primary

        // Fallback 1 — single-quoted name attribute
        val fallback1 = Regex("""<tool_call name='([^']+)'>([\s\S]*?)</tool_call>""")
            .findAll(text).map { ToolCall(it.groupValues[1], it.groupValues[2].trim()) }.toList()
        if (fallback1.isNotEmpty()) return fallback1

        // Fallback 2 — JSON block with "tool" and "args" keys (some models output this)
        val fallback2 = Regex(""""tool"\s*:\s*"([^"]+)"\s*,\s*"args"\s*:\s*(\{[^}]+\})""")
            .findAll(text).map { ToolCall(it.groupValues[1], it.groupValues[2].trim()) }.toList()
        if (fallback2.isNotEmpty()) return fallback2

        return emptyList()
    }

    // ── FIX 1: Robust arg parser ──────────────────────────────────────────────
    // Primary: JSON. Fallback: key-value regex extraction so NPEs never happen.
    private fun parseArgsRobust(raw: String): Map<String, String> {
        // Primary — valid JSON object
        return try {
            val obj = Json.parseToJsonElement(raw.trim()).jsonObject
            obj.mapValues { (_, v) ->
                when (v) {
                    is JsonPrimitive -> v.content
                    is JsonArray     -> v.toString()
                    else             -> v.toString()
                }
            }
        } catch (e: Exception) {
            // Fallback — extract "key": "value" or "key": value pairs
            val map = mutableMapOf<String, String>()
            val kvRegex = Regex(""""(\w+)"\s*:\s*(?:"([^"]*?)"|(\S+?))(?=[,}\s]|$)""")
            kvRegex.findAll(raw).forEach { m ->
                val key = m.groupValues[1]
                val value = m.groupValues[2].ifEmpty { m.groupValues[3] }
                map[key] = value
            }
            map
        }
    }

    // Helper: resolve a path arg (may be relative) against the project root
    private fun resolveArg(args: Map<String, String>, key: String, projectPath: String): String? {
        val v = args[key] ?: return null
        return resolvePath(v, projectPath)
    }

    private fun resolvePath(path: String, projectPath: String): String =
        if (path.startsWith("/")) path else File(projectPath, path).absolutePath

    // ── FIX 2: Dynamic context window ────────────────────────────────────────
    private fun buildConversationPrompt(
        history: List<Pair<String, String>>,
        current: String,
        maxHistoryChars: Int
    ): String {
        val trimmed = mutableListOf<Pair<String, String>>()
        var chars = 0
        for (pair in history.asReversed()) {
            val pairLen = pair.first.length + pair.second.length
            if (chars + pairLen > maxHistoryChars && trimmed.size >= 2) break
            trimmed.add(0, pair)
            chars += pairLen
        }
        val sb = StringBuilder()
        trimmed.forEach { (role, content) -> sb.appendLine("[$role]: $content") }
        sb.appendLine("[user]: $current")
        return sb.toString()
    }
}

sealed class AgentEvent {
    data class Thinking(val message: String) : AgentEvent()
    data class Token(val text: String) : AgentEvent()
    data class ToolCalling(val name: String, val args: String) : AgentEvent()
    data class ToolResult(val name: String, val output: String, val isError: Boolean = false) : AgentEvent()
    data class FinalAnswer(val text: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}
