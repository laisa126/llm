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

    // ── Gemma 3 ──────────────────────────────────────────────────────────────
    LLMModel(
        id = "gemma3-1b",
        name = "Gemma 3 1B",
        description = "Ultra-fast. Best for 3GB RAM phones. Great for coding tasks.",
        sizeGb = 0.7f, minRamGb = 3,
        supportsVision = false, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv1280.task",
        fileName = "gemma3-1b-q8.task"
    ),

    // ── Gemma 3n ─────────────────────────────────────────────────────────────
    LLMModel(
        id = "gemma3n-e2b",
        name = "Gemma 3n E2B",
        description = "Efficient 2B model with vision. Good balance. Needs 4GB RAM.",
        sizeGb = 1.5f, minRamGb = 4,
        supportsVision = true, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
        fileName = "gemma3n-e2b-int4.task"
    ),
    LLMModel(
        id = "gemma3n-e4b",
        name = "Gemma 3n E4B",
        description = "Efficient 4B model with vision. Best Gemma 3 quality. Needs 6GB RAM.",
        sizeGb = 2.0f, minRamGb = 6,
        supportsVision = true, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task",
        fileName = "gemma3n-e4b-int4.task"
    ),

    // ── Gemma 4 (LiteRT-LM .litertlm) ────────────────────────────────────────
    LLMModel(
        id = "gemma4-e2b",
        name = "Gemma 4 E2B",
        description = "Gemma 4 · 2B effective params · multimodal · 32K context. Needs 4GB RAM.",
        sizeGb = 2.58f, minRamGb = 4,
        supportsVision = true, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        fileName = "gemma-4-E2B-it.litertlm"
    ),
    LLMModel(
        id = "gemma4-e4b",
        name = "Gemma 4 E4B",
        description = "Gemma 4 · 4B effective params · best quality · 32K context. Needs 6GB RAM.",
        sizeGb = 3.65f, minRamGb = 6,
        supportsVision = true, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        fileName = "gemma-4-E4B-it.litertlm"
    ),

    // ── Phi-4 Mini ───────────────────────────────────────────────────────────
    LLMModel(
        id = "phi4-mini",
        name = "Phi-4 Mini",
        description = "Microsoft Phi-4 Mini · 3.8B · excellent at reasoning + code. Needs 5GB RAM.",
        sizeGb = 2.3f, minRamGb = 5,
        supportsVision = false, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct-LiteRT/resolve/main/phi-4-mini-instruct-int4.task",
        fileName = "phi-4-mini-int4.task"
    ),

    // ── Llama 3.2 ────────────────────────────────────────────────────────────
    LLMModel(
        id = "llama32-1b",
        name = "Llama 3.2 1B",
        description = "Meta Llama 3.2 1B · fast · solid general purpose. Needs 3GB RAM.",
        sizeGb = 0.8f, minRamGb = 3,
        supportsVision = false, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct/resolve/main/llama-3.2-1b-instruct-int4.task",
        fileName = "llama-3.2-1b-int4.task"
    ),
    LLMModel(
        id = "llama32-3b",
        name = "Llama 3.2 3B",
        description = "Meta Llama 3.2 3B · best Llama quality on device. Needs 5GB RAM.",
        sizeGb = 1.8f, minRamGb = 5,
        supportsVision = false, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct/resolve/main/llama-3.2-3b-instruct-int4.task",
        fileName = "llama-3.2-3b-int4.task"
    ),

    // ── Qwen 3.5 ─────────────────────────────────────────────────────────────
    LLMModel(
        id = "qwen35-2b",
        name = "Qwen 3.5 2B",
        description = "Alibaba Qwen 3.5 2B · great for code + multilingual. Needs 4GB RAM.",
        sizeGb = 1.5f, minRamGb = 4,
        supportsVision = false, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/Qwen3.5-2B-LiteRT/resolve/main/qwen3.5-2b-int4.task",
        fileName = "qwen3.5-2b-int4.task"
    ),
    LLMModel(
        id = "qwen35-4b",
        name = "Qwen 3.5 4B",
        description = "Alibaba Qwen 3.5 4B · best multilingual code model. Needs 6GB RAM.",
        sizeGb = 2.5f, minRamGb = 6,
        supportsVision = false, supportsCode = true,
        downloadUrl = "https://huggingface.co/litert-community/Qwen3.5-4B-LiteRT/resolve/main/qwen3.5-4b-int4.task",
        fileName = "qwen3.5-4b-int4.task"
    )
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
    val icon: String = "◆", val category: String = "code",
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

data class AppSettings(
    val serverPort: Int = 8080, val serverEnabled: Boolean = false,
    val activeModelId: String = "gemma3-1b", val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val systemPrompt: String = "You are an expert software engineer AI assistant.",
    val theme: String = "dark", val fontSize: Int = 14,
    val fontFamily: String = "sans", // "sans" | "serif" | "mono"
    val hfToken: String = "",
    val apiToken: String = "",  // Bearer token for LAN API; empty = loopback only (no auth enforced)
)
