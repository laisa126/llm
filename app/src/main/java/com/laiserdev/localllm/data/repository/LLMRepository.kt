package com.laiserdev.localllm.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class LLMRepository(private val context: Context) {

    private var engine: Engine? = null
    private var currentModelId: String? = null
    private var currentSupportsVision: Boolean = false
    private val TAG = "LLMRepository"

    // ── Load / unload ──────────────────────────────────────────────────────────

    suspend fun loadModel(
        modelId: String,
        fileName: String,
        supportsVision: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (currentModelId == modelId && engine != null) return@withContext Result.success(Unit)

            val modelFile = File(context.filesDir, "models/$fileName")
            if (!modelFile.exists()) return@withContext Result.failure(
                Exception("Model file not found. Download it from the Models tab.")
            )

            closeInternal()

            Log.d(TAG, "Loading: ${modelFile.absolutePath} (${modelFile.length() / 1_000_000}MB) vision=$supportsVision")

            val engineConfig = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.GPU(),
                cacheDir = context.cacheDir.absolutePath,
                visionBackend = if (supportsVision) Backend.GPU() else null
            )

            val newEngine = Engine(engineConfig)
            newEngine.initialize()

            engine = newEngine
            currentModelId = modelId
            currentSupportsVision = supportsVision
            Log.d(TAG, "[OK] Model loaded: $fileName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[ERR] Load failed", e)
            closeInternal()
            Result.failure(Exception("Failed to load model: ${e.message?.take(200)}"))
        }
    }

    fun isLoaded() = engine != null
    fun currentModel() = currentModelId
    fun supportsVision() = currentSupportsVision

    // ── Conversation factory ───────────────────────────────────────────────────
    // Official API: ConversationConfig(systemInstruction = Contents.of("..."), samplerConfig = ...)
    // A fresh Conversation is created per call so the system prompt can vary.

    private fun buildConversationConfig(systemPrompt: String) = ConversationConfig(
        systemInstruction = if (systemPrompt.isNotBlank()) Contents.of(systemPrompt) else null,
        samplerConfig = SamplerConfig(temperature = 0.7f, topK = 40)
    )

    // ── Text-only streaming ────────────────────────────────────────────────────
    fun generateStream(prompt: String, systemPrompt: String = ""): Flow<String> =
        generateStreamWithImage(prompt, systemPrompt, null)

    // ── Vision-aware streaming ─────────────────────────────────────────────────
    // Official API: sendMessageAsync(message: Message) returns Flow<Message>
    // Each emitted Message represents a streaming token chunk; Message.toString() yields the text.
    fun generateStreamWithImage(
        prompt: String,
        systemPrompt: String = "",
        imageUri: Uri? = null
    ): Flow<String> {
        val eng = engine ?: error("Engine not loaded")
        val conv = eng.createConversation(buildConversationConfig(systemPrompt))

        val userMessage = buildUserMessage(prompt, imageUri)

        return conv.sendMessageAsync(userMessage)
            .map { message -> message.toString() }
            .onCompletion { conv.close() }
            .catch { e -> throw e }
    }

    // ── Non-streaming generate ─────────────────────────────────────────────────
    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val eng = engine ?: return@withContext Result.failure(Exception("Engine not loaded"))
            eng.createConversation(buildConversationConfig(systemPrompt)).use { conv ->
                val response = conv.sendMessage(Message.of(prompt))
                Result.success(response.toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateCode(
        instruction: String,
        existingCode: String? = null,
        language: String = "auto"
    ): Result<String> {
        val system = "You are an expert software engineer. Return ONLY complete working code. Language: $language"
        val prompt = if (existingCode != null)
            "Modify:\n```\n$existingCode\n```\nInstruction: $instruction"
        else
            "Generate: $instruction"
        return generate(prompt, system, 4096)
    }

    suspend fun generateProject(description: String): Result<String> =
        generate(
            description,
            """Respond ONLY with JSON: {"projectName":"name","files":[{"path":"path","content":"content"}]}""",
            8192
        )

    // ── Message builder ────────────────────────────────────────────────────────

    private fun buildUserMessage(prompt: String, imageUri: Uri?): Message {
        if (imageUri == null || !currentSupportsVision) return Message.of(prompt)
        return try {
            val bitmap = loadBitmapFromUri(imageUri)
            if (bitmap != null) {
                val bytes = bitmapToJpegBytes(bitmap)
                Message.of(listOf(
                    Content.ImageBytes(bytes, "image/jpeg"),
                    Content.Text(prompt)
                ))
            } else {
                Log.w(TAG, "Could not decode image URI, falling back to text only")
                Message.of(prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image load error: ${e.message}")
            Message.of(prompt)
        }
    }

    // ── Image helpers ──────────────────────────────────────────────────────────

    private fun loadBitmapFromUri(uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Bitmap load error: ${e.message}")
        null
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap, maxEdge: Int = 1024): ByteArray {
        val scaled = if (bitmap.width > maxEdge || bitmap.height > maxEdge) {
            val ratio = maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else bitmap
        return ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }.toByteArray()
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    private fun closeInternal() {
        try { engine?.close() } catch (_: Exception) {}
        engine = null
        currentModelId = null
        currentSupportsVision = false
    }

    fun close() = closeInternal()
}
