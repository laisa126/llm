package com.laiserdev.localllm.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class LLMRepository(private val context: Context) {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentModelId: String? = null
    private var currentSupportsVision: Boolean = false
    private val TAG = "LLMRepository"

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
                // Only enable visionBackend for models that support it
                visionBackend = if (supportsVision) Backend.GPU() else null
            )

            val newEngine = Engine(engineConfig)
            newEngine.initialize()

            conversation = newEngine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(temperature = 0.7f, topK = 40)
                )
            )

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

    // ── Text-only streaming ────────────────────────────────────────────────────
    fun generateStream(prompt: String, systemPrompt: String = ""): Flow<String> =
        generateStreamWithImage(prompt, systemPrompt, null)

    // ── Vision-aware streaming ─────────────────────────────────────────────────
    fun generateStreamWithImage(
        prompt: String,
        systemPrompt: String = "",
        imageUri: Uri? = null
    ): Flow<String> = callbackFlow {
        val conv = conversation ?: throw Exception("No model loaded")
        conv.resetConversation()

        if (systemPrompt.isNotBlank()) {
            conv.addQueryChunk(Message.of(systemPrompt), isSystem = true)
        }

        // Build message — with or without image
        val userMessage = if (imageUri != null && currentSupportsVision) {
            try {
                val bitmap = loadBitmapFromUri(imageUri)
                if (bitmap != null) {
                    val bytes = bitmapToJpegBytes(bitmap)
                    // LiteRT-LM Message with image content
                    Message.of(
                        listOf(
                            Content.ImageBytes(bytes, "image/jpeg"),
                            Content.Text(prompt)
                        )
                    )
                } else {
                    Log.w(TAG, "Could not decode image URI, sending text only")
                    Message.of(prompt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image load error: ${e.message}")
                Message.of(prompt)
            }
        } else {
            Message.of(prompt)
        }

        val callback = object : MessageCallback {
            override fun onMessage(message: Message) {
                message.content.forEach { part ->
                    val text = part.text
                    if (!text.isNullOrEmpty()) trySend(text)
                }
            }
            override fun onDone() { close() }
            override fun onError(throwable: Throwable) { close(throwable) }
        }

        conv.sendMessageAsync(userMessage, callback)
        awaitClose {}
    }

    // ── Non-streaming generate ─────────────────────────────────────────────────
    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        val conv = conversation ?: return@withContext Result.failure(Exception("No model loaded"))
        try {
            conv.resetConversation()
            if (systemPrompt.isNotBlank()) conv.addQueryChunk(Message.of(systemPrompt), isSystem = true)
            val response = conv.sendMessage(Message.of(prompt))
            val text = response.content.joinToString("") { it.text ?: "" }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateCode(instruction: String, existingCode: String? = null, language: String = "auto"): Result<String> {
        val system = "You are an expert software engineer. Return ONLY complete working code. Language: $language"
        val prompt = if (existingCode != null) "Modify:\n```\n$existingCode\n```\nInstruction: $instruction"
                     else "Generate: $instruction"
        return generate(prompt, system, 4096)
    }

    suspend fun generateProject(description: String): Result<String> =
        generate(description,
            """Respond ONLY with JSON: {"projectName":"name","files":[{"path":"path","content":"content"}]}""",
            8192)

    // ── Image helpers ──────────────────────────────────────────────────────────
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap load error: ${e.message}")
            null
        }
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap, maxEdge: Int = 1024): ByteArray {
        // Downscale if too large — vision models don't benefit from huge images
        val scaled = if (bitmap.width > maxEdge || bitmap.height > maxEdge) {
            val ratio = maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    }

    private fun closeInternal() {
        try { conversation?.resetConversation() } catch (_: Exception) {}
        conversation = null
        try { engine?.close() } catch (_: Exception) {}
        engine = null
        currentModelId = null
        currentSupportsVision = false
    }

    fun close() = closeInternal()
}
