package com.laiserdev.localllm.util

import android.content.Context
import kotlinx.coroutines.flow.toList
import java.io.File

/**
 * PackageManager wraps npm, pip, and termux-pkg (apt).
 * Detects which runtime is available and runs installs.
 */
class PackageManager(private val context: Context) {

    private val executor = TerminalExecutor(context)

    // ─── Install ──────────────────────────────────────────────────────────────

    suspend fun install(manager: String, packages: List<String>, projectPath: String = ""): String {
        if (packages.isEmpty()) return "No packages specified"
        val pkgList = packages.joinToString(" ")

        val command = when (manager.lowercase()) {
            "npm"  -> buildNpmInstall(packages, projectPath)
            "pip"  -> "pip3 install $pkgList"
            "pip3" -> "pip3 install $pkgList"
            "apt"  -> "apt-get install -y $pkgList"
            "pkg"  -> "pkg install -y $pkgList" // Termux
            "yarn" -> "yarn add $pkgList"
            "npx"  -> "npx $pkgList"
            else   -> return "❌ Unknown package manager: $manager. Use npm, pip, apt, or yarn."
        }

        if (projectPath.isNotBlank()) executor.workingDir = File(projectPath)

        val output = StringBuilder()
        executor.execute(command).collect { (line, isErr) ->
            output.appendLine(if (isErr) "⚠ $line" else line)
        }
        return output.toString().trim()
    }

    private fun buildNpmInstall(packages: List<String>, projectPath: String): String {
        // Check if package.json exists — if not, init first
        val hasPackageJson = projectPath.isNotBlank() &&
                File(projectPath, "package.json").exists()
        val pkgList = packages.joinToString(" ")
        return if (hasPackageJson) "npm install $pkgList"
        else "npm init -y && npm install $pkgList"
    }

    // ─── Check if installed ───────────────────────────────────────────────────

    suspend fun isInstalled(tool: String): Boolean {
        val output = mutableListOf<Pair<String, Boolean>>()
        executor.execute("which $tool").toList().let { output.addAll(it) }
        return output.any { (line, isErr) -> !isErr && line.contains("/") }
    }

    // ─── Detect runtimes available ────────────────────────────────────────────

    suspend fun detectRuntimes(): Map<String, Boolean> {
        return mapOf(
            "node"   to isInstalled("node"),
            "npm"    to isInstalled("npm"),
            "python3" to isInstalled("python3"),
            "pip3"   to isInstalled("pip3"),
            "git"    to isInstalled("git"),
            "curl"   to isInstalled("curl"),
            "wget"   to isInstalled("wget"),
            "make"   to isInstalled("make"),
            "gcc"    to isInstalled("gcc"),
            "java"   to isInstalled("java"),
            "ruby"   to isInstalled("ruby"),
            "go"     to isInstalled("go"),
            "rustc"  to isInstalled("rustc")
        )
    }

    // ─── Run project with auto-detected runner ────────────────────────────────

    fun detectRunCommand(projectPath: String): String? {
        val dir = File(projectPath)
        return when {
            File(dir, "package.json").exists() -> {
                val pkg = File(dir, "package.json").readText()
                if (pkg.contains("\"dev\"")) "npm run dev"
                else if (pkg.contains("\"start\"")) "npm start"
                else "node index.js"
            }
            File(dir, "requirements.txt").exists() -> "pip3 install -r requirements.txt && python3 main.py"
            File(dir, "manage.py").exists() -> "python3 manage.py runserver 0.0.0.0:8000"
            File(dir, "Makefile").exists() -> "make"
            File(dir, "Cargo.toml").exists() -> "cargo run"
            File(dir, "go.mod").exists() -> "go run ."
            File(dir, "index.html").exists() -> "echo 'Static site — open index.html in preview'"
            else -> null
        }
    }
}
