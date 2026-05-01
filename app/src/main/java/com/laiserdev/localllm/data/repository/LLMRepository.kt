package com.laiserdev.localllm.data.repository

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LLMRepository(private val context: Context) {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentModelId: String? = null
    private val TAG = "LLMRepository"

    suspend fun loadModel(modelId: String, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (currentModelId == modelId && engine != null) return@withContext Result.success(Unit)

                val modelFile = File(context.filesDir, "models/$fileName")
                if (!modelFile.exists()) {
                    return@withContext Result.failure(
                        Exception("Model file not found. Please download it first from the Models tab.")
                    )
                }

                // Close existing engine before loading new one
                closeInternal()

                Log.d(TAG, "Loading model: ${modelFile.absolutePath} (${modelFile.length() / 1_000_000}MB)")

                val engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.GPU(),   // falls back to CPU automatically if GPU unavailable
                    cacheDir = context.cacheDir.absolutePath
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
                Log.d(TAG, "✅ Model loaded: $fileName")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Load failed", e)
                closeInternal()
                Result.failure(Exception("Failed to load model: ${e.message?.take(200) ?: "Unknown error"}"))
            }
        }

    fun isLoaded() = engine != null
    fun currentModel() = currentModelId

    /**
     * True streaming using LiteRT-LM's sendMessageAsync + MessageCallback.
     * Tokens arrive in real time as the model generates them.
     */
    fun generateStream(prompt: String, systemPrompt: String = ""): Flow<String> = callbackFlow {
        val conv = conversation ?: throw Exception("No model loaded")

        // Reset conversation for fresh context (keeps memory lean)
        conv.resetConversation()

        // Inject system prompt as first turn if provided
        if (systemPrompt.isNotBlank()) {
            conv.addQueryChunk(Message.of(systemPrompt), isSystem = true)
        }

        val userMessage = Message.of(prompt)

        val callback = object : MessageCallback {
            override fun onMessage(message: Message) {
                // Extract text tokens from the message
                message.content.forEach { part ->
                    val text = part.text
                    if (!text.isNullOrEmpty()) {
                        trySend(text)
                    }
                }
            }
            override fun onDone() {
                close()
            }
            override fun onError(throwable: Throwable) {
                close(throwable)
            }
        }

        conv.sendMessageAsync(userMessage, callback)

        awaitClose {
            // callbackFlow cleanup — nothing to cancel explicitly in LiteRT-LM 
        }
    }

    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        val conv = conversation ?: return@withContext Result.failure(Exception("No model loaded"))
        try {
            conv.resetConversation()
            if (systemPrompt.isNotBlank()) {
                conv.addQueryChunk(Message.of(systemPrompt), isSystem = true)
            }
            val response = conv.sendMessage(Message.of(prompt))
            val text = response.content.joinToString("") { it.text ?: "" }
            Result.success(text)
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
        else "Generate: $instruction"
        return generate(prompt, system, 4096)
    }

    suspend fun generateProject(description: String): Result<String> =
        generate(
            description,
            """Respond ONLY with JSON: {"projectName":"name","files":[{"path":"path","content":"content"}]}""",
            8192
        )

    private fun closeInternal() {
        try { conversation?.resetConversation() } catch (_: Exception) {}
        conversation = null
        try { engine?.close() } catch (_: Exception) {}
        engine = null
        currentModelId = null
    }

    fun close() {
        closeInternal()
    }
}
