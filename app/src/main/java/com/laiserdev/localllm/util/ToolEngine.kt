package com.laiserdev.localllm.util

import android.content.Context
import com.laiserdev.localllm.data.model.ToolCall
import com.laiserdev.localllm.data.model.ToolResult
import com.laiserdev.localllm.data.repository.LLMRepository
import com.laiserdev.localllm.data.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * ToolEngine lets the LLM call real tools:
 *   read_file, write_file, list_files, run_command,
 *   install_package, search_web, create_file, delete_file,
 *   apply_diff, grep_files, get_file_info, make_dir
 *
 * The agentic loop:
 *   1. Send prompt + tool definitions to LLM
 *   2. Parse <tool_call> blocks from LLM output
 *   3. Execute the tool
 *   4. Feed result back to LLM
 *   5. Repeat until LLM gives a final answer (no tool calls)
 */
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

1. read_file       {"path": "absolute or relative path"}
2. write_file      {"path": "...", "content": "full file content"}
3. create_file     {"path": "...", "content": ""}
4. delete_file     {"path": "..."}
5. list_files      {"path": "directory path", "recursive": false}
6. make_dir        {"path": "..."}
7. apply_diff      {"path": "...", "old": "text to replace", "new": "replacement"}
8. grep_files      {"path": "dir", "pattern": "regex", "recursive": true}
9. get_file_info   {"path": "..."}
10. run_command    {"command": "shell command", "cwd": "optional working dir"}
11. install_package {"manager": "npm|pip|apt", "packages": ["pkg1","pkg2"]}
12. search_web     {"query": "search query"}
13. http_get       {"url": "https://..."}
14. list_processes  {"filter": "optional name filter"}
15. kill_process    {"pid": 1234}
16. download_zip    {"url": "https://...", "dest_dir": "relative or absolute path"}
    Downloads a ZIP from a URL and extracts it to dest_dir. Handles redirects.
17. screenshot      {"path": "optional output path (default: screenshots/capture.png)"}
    Captures the current Preview tab WebView as a PNG. Use to visually verify your UI.

Rules:
- Always use tools to read files before editing them
- After writing code, run it to verify it works
- If a package is missing, install it first
- Return a final answer ONLY when all tool calls are complete
""".trimIndent()
    }

    // ─── Agentic Loop ─────────────────────────────────────────────────────────

    fun agentLoop(
        userPrompt: String,
        projectPath: String,
        systemExtra: String = "",
        maxIterations: Int = 10
    ): Flow<AgentEvent> = channelFlow {
        terminal.workingDir = File(projectPath)

        val system = buildString {
            appendLine("You are an expert AI coding assistant with full access to the file system.")
            appendLine("Project path: $projectPath")
            if (systemExtra.isNotBlank()) appendLine(systemExtra)
            appendLine()
            appendLine(TOOL_DEFINITIONS)
        }

        var conversationHistory = mutableListOf<Pair<String, String>>() // role, content
        var currentPrompt = userPrompt
        var iterations = 0

        send(AgentEvent.Thinking("Analyzing task..."))

        while (iterations < maxIterations) {
            iterations++

            val fullPrompt = buildConversationPrompt(conversationHistory, currentPrompt)
            val responseBuilder = StringBuilder()
            send(AgentEvent.Thinking("Generating response (step $iterations)..."))

            llm.generateStream(fullPrompt, system).collect { token ->
                responseBuilder.append(token)
                // Only stream tokens when we don't yet know if it's a tool call or final answer
                // Tokens are streamed live; FinalAnswer is sent at the end WITHOUT re-emitting tokens
            }

            val response = responseBuilder.toString()
            conversationHistory.add(Pair("user", currentPrompt))
            conversationHistory.add(Pair("assistant", response))

            val toolCalls = parseToolCalls(response)

            if (toolCalls.isEmpty()) {
                // Strip any accidental tool tags from final answer and emit it once
                val clean = response
                    .replace(Regex("<tool_call[^>]*>[\\s\\S]*?</tool_call>"), "")
                    .trim()
                send(AgentEvent.FinalAnswer(clean))
                break
            }

            // Stream tokens only for intermediate steps (tool-calling rounds)
            response.split(Regex("(?<=\\s)|(?=\\s)")).forEach { token ->
                if (!token.contains("<tool_call")) send(AgentEvent.Token(token))
            }

            val toolResults = StringBuilder()
            for (tc in toolCalls) {
                send(AgentEvent.ToolCalling(tc.name, tc.args))
                val result = executeTool(tc, projectPath)
                send(AgentEvent.ToolResult(tc.name, result.output, result.isError))
                toolResults.appendLine("<tool_result name=\"${tc.name}\">")
                toolResults.appendLine(result.output)
                toolResults.appendLine("</tool_result>")
            }

            currentPrompt = toolResults.toString().trim()
        }

        if (iterations >= maxIterations) {
            send(AgentEvent.Error("Max iterations reached. Task may be incomplete."))
        }
    }

    // ─── Tool Execution ───────────────────────────────────────────────────────

    private suspend fun executeTool(tc: ToolCall, projectPath: String): ToolResult {
        return try {
            val args = Json.parseToJsonElement(tc.args).jsonObject
            when (tc.name) {
                "read_file" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val content = File(path).readText()
                    ToolResult("```\n$content\n```")
                }
                "write_file" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val content = args["content"]!!.jsonPrimitive.content
                    File(path).also { it.parentFile?.mkdirs() }.writeText(content)
                    ToolResult("✅ Written: $path (${content.length} chars)")
                }
                "create_file" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val content = args["content"]?.jsonPrimitive?.content ?: ""
                    val f = File(path)
                    f.parentFile?.mkdirs()
                    f.writeText(content)
                    ToolResult("✅ Created: $path")
                }
                "delete_file" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val f = File(path)
                    val deleted = if (f.isDirectory) f.deleteRecursively() else f.delete()
                    ToolResult(if (deleted) "✅ Deleted: $path" else "❌ Could not delete: $path", !deleted)
                }
                "list_files" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val recursive = args["recursive"]?.jsonPrimitive?.booleanOrNull ?: false
                    val files = if (recursive) {
                        File(path).walkTopDown().map {
                            "${if (it.isDirectory) "📁" else "📄"} ${it.relativeTo(File(path))}"
                        }.joinToString("\n")
                    } else {
                        File(path).listFiles()?.joinToString("\n") {
                            "${if (it.isDirectory) "📁" else "📄"} ${it.name}"
                        } ?: "Empty directory"
                    }
                    ToolResult(files)
                }
                "make_dir" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    File(path).mkdirs()
                    ToolResult("✅ Directory created: $path")
                }
                "apply_diff" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val old = args["old"]!!.jsonPrimitive.content
                    val new = args["new"]!!.jsonPrimitive.content
                    val f = File(path)
                    val original = f.readText()
                    if (!original.contains(old)) {
                        ToolResult("❌ Pattern not found in file", isError = true)
                    } else {
                        f.writeText(original.replace(old, new))
                        ToolResult("✅ Diff applied to $path")
                    }
                }
                "grep_files" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
                    val pattern = args["pattern"]!!.jsonPrimitive.content
                    val recursive = args["recursive"]?.jsonPrimitive?.booleanOrNull ?: true
                    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                    val results = mutableListOf<String>()
                    val root = File(path)
                    val files = if (recursive) root.walkTopDown().filter { it.isFile }
                    else root.listFiles()?.asSequence()?.filter { it.isFile } ?: emptySequence()
                    files.forEach { f ->
                        f.readLines().forEachIndexed { i, line ->
                            if (regex.containsMatchIn(line)) {
                                results.add("${f.relativeTo(root)}:${i + 1}: $line")
                            }
                        }
                    }
                    ToolResult(if (results.isEmpty()) "No matches found" else results.joinToString("\n"))
                }
                "get_file_info" -> {
                    val path = resolvePath(args["path"]!!.jsonPrimitive.content, projectPath)
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
                    val command = args["command"]!!.jsonPrimitive.content
                    val cwd = args["cwd"]?.jsonPrimitive?.content
                    if (cwd != null) terminal.workingDir = File(resolvePath(cwd, projectPath))
                    val output = StringBuilder()
                    terminal.execute(command).collect { (line, isErr) ->
                        output.appendLine(if (isErr) "stderr: $line" else line)
                    }
                    ToolResult(output.toString().trim())
                }
                "install_package" -> {
                    val manager = args["manager"]!!.jsonPrimitive.content
                    val packages = args["packages"]!!.jsonArray.map { it.jsonPrimitive.content }
                    val result = pkgManager.install(manager, packages, projectPath)
                    ToolResult(result)
                }
                "search_web" -> {
                    val query = args["query"]!!.jsonPrimitive.content
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        URL("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1").readText()
                    }
                    val json = Json.parseToJsonElement(response).jsonObject
                    val answer = json["AbstractText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        ?: json["Answer"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        ?: "No direct answer found. Try a more specific query."
                    ToolResult(answer)
                }
                "http_get" -> {
                    val url = args["url"]!!.jsonPrimitive.content
                    val content = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        URL(url).readText().take(4000)
                    }
                    ToolResult(content)
                }
                "list_processes" -> {
                    val filter = args["filter"]?.jsonPrimitive?.content ?: ""
                    val output = StringBuilder()
                    terminal.execute("ps aux").collect { (line, _) ->
                        if (filter.isEmpty() || line.contains(filter, ignoreCase = true)) {
                            output.appendLine(line)
                        }
                    }
                    ToolResult(output.toString())
                }
                "kill_process" -> {
                    val pid = args["pid"]!!.jsonPrimitive.int
                    val output = StringBuilder()
                    terminal.execute("kill $pid").collect { (line, _) -> output.appendLine(line) }
                    ToolResult(output.toString().ifBlank { "✅ Killed PID $pid" })
                }
                "download_zip" -> {
                    val url = args["url"]!!.jsonPrimitive.content
                    val destDir = resolvePath(
                        args["dest_dir"]?.jsonPrimitive?.content ?: "downloads",
                        projectPath
                    )
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
                        if (conn.responseCode !in 200..299) {
                            return@withContext ToolResult(
                                "❌ HTTP ${conn.responseCode} from $url", isError = true
                            )
                        }
                        val totalBytes = conn.contentLengthLong
                        var extracted = 0
                        val extractedFiles = mutableListOf<String>()
                        conn.inputStream.use { raw ->
                            ZipInputStream(raw.buffered(65536)).use { zip ->
                                var entry = zip.nextEntry
                                while (entry != null) {
                                    val outFile = File(destDir, entry.name)
                                    if (entry.isDirectory) {
                                        outFile.mkdirs()
                                    } else {
                                        outFile.parentFile?.mkdirs()
                                        FileOutputStream(outFile).use { fos ->
                                            zip.copyTo(fos)
                                        }
                                        extractedFiles.add(entry.name)
                                        extracted++
                                    }
                                    zip.closeEntry()
                                    entry = zip.nextEntry
                                }
                            }
                        }
                        ToolResult(buildString {
                            appendLine("✅ Downloaded and extracted $extracted files to $destDir")
                            if (extractedFiles.size <= 20) {
                                appendLine("Files:")
                                extractedFiles.forEach { appendLine("  • $it") }
                            } else {
                                appendLine("First 20 files:")
                                extractedFiles.take(20).forEach { appendLine("  • $it") }
                                appendLine("  ... and ${extractedFiles.size - 20} more")
                            }
                        })
                    }
                }
                "screenshot" -> {
                    val relPath = args["path"]?.jsonPrimitive?.content ?: "screenshots/capture.png"
                    val outputPath = resolvePath(relPath, projectPath)
                    // WebView.draw() must run on main thread
                    var result: ToolResult? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    Handler(Looper.getMainLooper()).post {
                        result = try {
                            val captureResult = com.laiserdev.localllm.util.WebViewRegistry
                                .captureToFile(outputPath)
                            if (captureResult.isSuccess) {
                                ToolResult(
                                    "✅ Screenshot saved to ${captureResult.getOrNull()}
" +
                                    "Use read_file to analyze it, or view it in the Editor."
                                )
                            } else {
                                ToolResult(
                                    "❌ Screenshot failed: ${captureResult.exceptionOrNull()?.message}",
                                    isError = true
                                )
                            }
                        } catch (e: Exception) {
                            ToolResult("❌ Screenshot error: ${e.message}", isError = true)
                        }
                        latch.countDown()
                    }
                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    result ?: ToolResult("❌ Screenshot timed out", isError = true)
                }
                else -> ToolResult("❌ Unknown tool: ${tc.name}", isError = true)
            }
        } catch (e: Exception) {
            ToolResult("❌ Tool error: ${e.message}", isError = true)
        }
    }

    // ─── Parse <tool_call> blocks from LLM output ─────────────────────────────

    private fun parseToolCalls(text: String): List<ToolCall> {
        val regex = Regex("""<tool_call name="([^"]+)">([\s\S]*?)</tool_call>""")
        return regex.findAll(text).map { match ->
            ToolCall(name = match.groupValues[1], args = match.groupValues[2].trim())
        }.toList()
    }

    private fun buildConversationPrompt(
        history: List<Pair<String, String>>,
        current: String
    ): String {
        // Trim history to stay within ~3000 chars to avoid overflowing
        // the 4096-token context window of Gemma 3 1B.
        // Always keep the FIRST exchange (original task) + the most recent N pairs.
        val MAX_HISTORY_CHARS = 3000
        val trimmed = mutableListOf<Pair<String, String>>()
        var chars = 0
        // Walk backwards, keeping the most recent pairs first
        for (pair in history.asReversed()) {
            val pairLen = pair.first.length + pair.second.length
            if (chars + pairLen > MAX_HISTORY_CHARS && trimmed.size >= 2) break
            trimmed.add(0, pair)
            chars += pairLen
        }
        val sb = StringBuilder()
        trimmed.forEach { (role, content) -> sb.appendLine("[$role]: $content") }
        sb.appendLine("[user]: $current")
        return sb.toString()
    }

    private fun resolvePath(path: String, projectPath: String): String {
        return if (path.startsWith("/")) path
        else File(projectPath, path).absolutePath
    }
}

// ─── Events emitted during agentic loop ───────────────────────────────────────

sealed class AgentEvent {
    data class Thinking(val message: String) : AgentEvent()
    data class Token(val text: String) : AgentEvent()
    data class ToolCalling(val name: String, val args: String) : AgentEvent()
    data class ToolResult(val name: String, val output: String, val isError: Boolean = false) : AgentEvent()
    data class FinalAnswer(val text: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}
