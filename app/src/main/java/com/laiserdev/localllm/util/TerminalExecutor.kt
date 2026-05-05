package com.laiserdev.localllm.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File

class TerminalExecutor(private val context: Context) {

    private var process: Process? = null
    var workingDir: File = context.filesDir

    fun execute(command: String): Flow<Pair<String, Boolean>> = callbackFlow {
        try {
            val shell = listOf("/system/bin/sh", "-c", command)
            val pb = ProcessBuilder(shell)
                .directory(workingDir)
                .redirectErrorStream(false)

            // Set environment
            pb.environment().apply {
                put("HOME", context.filesDir.absolutePath)
                put("TMPDIR", context.cacheDir.absolutePath)
                put("PATH", "/system/bin:/system/xbin")
            }

            process = pb.start()
            val proc = process ?: return@launch

            // Stdout reader
            launch(Dispatchers.IO) {
                proc.inputStream.bufferedReader().forEachLine { line ->
                    trySend(Pair(line, false))
                }
            }

            // Stderr reader
            launch(Dispatchers.IO) {
                proc.errorStream.bufferedReader().forEachLine { line ->
                    trySend(Pair(line, true))
                }
            }

            // Wait for completion
            launch(Dispatchers.IO) {
                val exitCode = proc.waitFor()
                trySend(Pair("Process exited with code $exitCode", exitCode != 0))
                close()
            }

        } catch (e: Exception) {
            trySend(Pair("Error: ${e.message}", true))
            close()
        }

        awaitClose { process?.destroy() }
    }

    fun killCurrentProcess() {
        process?.destroyForcibly()
        process = null
    }

    // Built-in commands that bypass shell
    fun handleBuiltin(command: String, currentPath: String): Pair<String, String>? {
        val parts = command.trim().split(Regex("\\s+"))
        return when (parts[0]) {
            "cd" -> {
                val target = if (parts.size > 1) parts[1] else context.filesDir.absolutePath
                val newDir = if (target.startsWith("/")) File(target)
                else File(workingDir, target)
                return if (newDir.exists() && newDir.isDirectory) {
                    workingDir = newDir
                    Pair(newDir.absolutePath, "")
                } else {
                    Pair(currentPath, "cd: ${newDir.absolutePath}: No such directory")
                }
            }
            "pwd" -> Pair(currentPath, workingDir.absolutePath)
            "clear" -> Pair(currentPath, "\u001b[2J") // ANSI clear
            "ls" -> {
                val dir = if (parts.size > 1) File(workingDir, parts[1]) else workingDir
                val listing = dir.listFiles()
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                    ?.joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
                    ?: "Cannot list directory"
                Pair(currentPath, listing)
            }
            else -> null
        }
    }
}
