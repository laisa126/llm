package com.laiserdev.localllm.data.model

import kotlinx.serialization.Serializable

enum class MessageRole { USER, ASSISTANT, SYSTEM }

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val tokensPerSecond: Float = 0f,
    val toolCalls: List<ToolCall> = emptyList()
)

data class ToolCall(val name: String, val args: String)
data class ToolResult(val output: String, val isError: Boolean = false)

enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, READY, LOADING, LOADED, ERROR }

data class LLMModel(
    val id: String, val name: String, val description: String,
    val sizeGb: Float, val minRamGb: Int,
    val supportsVision: Boolean, val supportsCode: Boolean,
    val downloadUrl: String, val fileName: String,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f, val errorMessage: String? = null
)

val AVAILABLE_MODELS = listOf(
    LLMModel("gemma3-1b","Gemma 3 1B","Ultra-fast. ~700MB.",0.7f,3,false,true,
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task","gemma3-1b-int4.task"),
    LLMModel("gemma3-4b","Gemma 3 4B","Vision + code. 6GB+ RAM. ~2.5GB.",2.5f,6,true,true,
        "https://huggingface.co/litert-community/Gemma3-4B-IT/resolve/main/gemma3-4b-it-int4.task","gemma3-4b-int4.task"),
    LLMModel("gemma4-4b","Gemma 4 4B","Best reasoning+vision. 8GB+ RAM. ~2.7GB.",2.7f,8,true,true,
        "https://huggingface.co/litert-community/Gemma4-4B-IT/resolve/main/gemma4-4b-it-int4.task","gemma4-4b-int4.task")
)

data class Project(
    val id: String = java.util.UUID.randomUUID().toString(), val name: String,
    val description: String = "", val path: String,
    val createdAt: Long = System.currentTimeMillis(), val lastModified: Long = System.currentTimeMillis(),
    val language: String = "auto"
)

data class ProjectFile(
    val name: String, val path: String, val absolutePath: String,
    val isDirectory: Boolean, val children: List<ProjectFile> = emptyList(),
    val size: Long = 0L, val extension: String = name.substringAfterLast(".", "")
)

data class TerminalLine(
    val text: String, val type: LineType = LineType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis()
) { enum class LineType { COMMAND, OUTPUT, ERROR, INFO, TOOL } }

@Serializable
data class Skill(
    val id: String, val name: String, val description: String,
    val icon: String = "⚡", val category: String = "code",
    val systemPrompt: String, val userPromptTemplate: String,
    val isBuiltin: Boolean = false
)

@Serializable
data class ApiRequest(val prompt: String, val system: String? = null,
    val maxTokens: Int = 1024, val stream: Boolean = false, val mode: String = "chat")

@Serializable
data class ApiResponse(val id: String, val content: String, val model: String,
    val tokensUsed: Int = 0, val latencyMs: Long = 0)

@Serializable
data class ApiError(val error: String, val code: Int = 400)

@Serializable
data class ApiKeyRecord(val id: String = "", val key: String = "", val name: String = "",
    val email: String = "", val usageCount: Int = 0, val isActive: Boolean = true,
    val createdAt: String = "")

data class AppSettings(
    val serverPort: Int = 8080, val serverEnabled: Boolean = false,
    val activeModelId: String = "gemma3-1b", val maxTokens: Int = 2048,
    val temperature: Float = 0.7f,
    val systemPrompt: String = "You are an expert software engineer AI assistant.",
    val theme: String = "dark", val fontSize: Int = 14
)
