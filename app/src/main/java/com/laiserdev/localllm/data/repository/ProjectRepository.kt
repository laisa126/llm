package com.laiserdev.localllm.data.repository

import android.content.Context
import android.net.Uri
import com.laiserdev.localllm.data.model.Project
import com.laiserdev.localllm.data.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.lingala.zip4j.ZipFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProjectRepository(private val context: Context) {

    private val projectsDir = File(context.filesDir, "projects").also { it.mkdirs() }

    // ─── List Projects ────────────────────────────────────────────────────────

    suspend fun listProjects(): List<Project> = withContext(Dispatchers.IO) {
        projectsDir.listFiles()?.filter { it.isDirectory }?.map { dir ->
            Project(
                id = dir.name,
                name = dir.name,
                path = dir.absolutePath,
                lastModified = dir.lastModified()
            )
        }?.sortedByDescending { it.lastModified } ?: emptyList()
    }

    // ─── Create Project ───────────────────────────────────────────────────────

    suspend fun createProject(name: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val safeName = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val dir = File(projectsDir, safeName).also { it.mkdirs() }
            val project = Project(id = safeName, name = name, path = dir.absolutePath)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Delete Project ───────────────────────────────────────────────────────

    suspend fun deleteProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(projectsDir, projectId).deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── File Tree ────────────────────────────────────────────────────────────

    suspend fun getFileTree(projectPath: String): ProjectFile = withContext(Dispatchers.IO) {
        buildFileTree(File(projectPath))
    }

    private fun buildFileTree(file: File): ProjectFile {
        val children = if (file.isDirectory) {
            file.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { buildFileTree(it) }
                ?: emptyList()
        } else emptyList()

        return ProjectFile(
            name = file.name,
            path = file.name,
            absolutePath = file.absolutePath,
            isDirectory = file.isDirectory,
            children = children,
            size = if (file.isFile) file.length() else 0L
        )
    }

    // ─── Read / Write Files ───────────────────────────────────────────────────

    suspend fun readFile(absolutePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(File(absolutePath).readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFile(absolutePath: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(absolutePath)
                file.parentFile?.mkdirs()
                file.writeText(content)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createFile(projectPath: String, relativePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(projectPath, relativePath)
                file.parentFile?.mkdirs()
                file.createNewFile()
                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteFile(absolutePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(absolutePath).let {
                if (it.isDirectory) it.deleteRecursively() else it.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(absolutePath: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val old = File(absolutePath)
                val new = File(old.parent, newName)
                old.renameTo(new)
                Result.success(new.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── ZIP Export ───────────────────────────────────────────────────────────

    suspend fun exportProjectAsZip(projectPath: String): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val projectDir = File(projectPath)
                val exportsDir = File(context.filesDir, "exports").also { it.mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val zipFile = File(exportsDir, "${projectDir.name}_$timestamp.zip")

                ZipFile(zipFile).addFolder(projectDir)
                Result.success(zipFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── ZIP Import ───────────────────────────────────────────────────────────

    suspend fun importProjectFromZip(zipUri: Uri): Result<Project> =
        withContext(Dispatchers.IO) {
            try {
                // Copy to temp
                val tempZip = File(context.cacheDir, "import_${System.currentTimeMillis()}.zip")
                context.contentResolver.openInputStream(zipUri)?.use { input ->
                    tempZip.outputStream().use { input.copyTo(it) }
                } ?: return@withContext Result.failure(Exception("Cannot open zip"))

                val zipFile = ZipFile(tempZip)
                val projectName = tempZip.nameWithoutExtension
                    .replace(Regex("_\\d{8}_\\d{6}$"), "") // strip timestamp
                val destDir = File(projectsDir, projectName).also { it.mkdirs() }

                zipFile.extractAll(destDir.absolutePath)
                tempZip.delete()

                val project = Project(
                    id = projectName,
                    name = projectName,
                    path = destDir.absolutePath
                )
                Result.success(project)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── AI-Generated Project ─────────────────────────────────────────────────

    suspend fun createFromAIJson(jsonString: String): Result<Project> =
        withContext(Dispatchers.IO) {
            try {
                // Strip markdown code fences if present
                val clean = jsonString
                    .trimIndent()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()

                val json = Json.parseToJsonElement(clean).jsonObject
                val projectName = json["projectName"]?.jsonPrimitive?.content
                    ?: "ai_project_${System.currentTimeMillis()}"
                val files = json["files"]?.jsonArray ?: return@withContext Result.failure(
                    Exception("No files in AI response")
                )

                val safeName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val projectDir = File(projectsDir, safeName).also { it.mkdirs() }

                files.forEach { fileElement ->
                    val fileObj = fileElement.jsonObject
                    val path = fileObj["path"]?.jsonPrimitive?.content ?: return@forEach
                    val content = fileObj["content"]?.jsonPrimitive?.content ?: ""
                    val file = File(projectDir, path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                }

                Result.success(
                    Project(id = safeName, name = projectName, path = projectDir.absolutePath)
                )
            } catch (e: Exception) {
                Result.failure(Exception("Failed to parse AI project: ${e.message}"))
            }
        }
}
