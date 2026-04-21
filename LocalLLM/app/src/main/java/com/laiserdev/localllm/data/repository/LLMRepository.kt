package com.laiserdev.localllm.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

class LLMRepository(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var currentModelId: String? = null
    private val TAG = "LLMRepository"

    // ─── Load Model ───────────────────────────────────────────────────────────

    suspend fun loadModel(modelId: String, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (currentModelId == modelId && llmInference != null) {
                    return@withContext Result.success(Unit) // already loaded
                }
                val modelFile = File(context.filesDir, "models/$fileName")
                if (!modelFile.exists()) {
                    return@withContext Result.failure(Exception("Model not downloaded: $fileName"))
                }
                llmInference?.close()
                llmInference = null

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(4096)
                    .setPreferredBackend(LlmInference.Backend.GPU)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                currentModelId = modelId
                Log.d(TAG, "✅ Model loaded: $fileName")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load model", e)
                Result.failure(e)
            }
        }

    fun isLoaded() = llmInference != null
    fun currentModel() = currentModelId

    // ─── Streaming Generation ─────────────────────────────────────────────────

    fun generateStream(
        prompt: String,
        systemPrompt: String = "",
        imageUri: Uri? = null,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<String> = callbackFlow {
        val inference = llmInference
        if (inference == null) {
            close(Exception("No model loaded. Please download and select a model first."))
            return@callbackFlow
        }

        try {
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(temperature)
                .setTopK(40)
                .setTopP(0.95f)
                .build()

            val session = LlmInferenceSession.createFromLlmInference(inference, sessionOptions)

            // Build prompt with Gemma chat format
            val fullPrompt = buildGemmaPrompt(systemPrompt, prompt)
            session.addQueryChunk(fullPrompt)

            // Attach image for vision models
            if (imageUri != null) {
                loadBitmap(imageUri)?.let { session.addImage(it) }
            }

            // Stream tokens
            session.generateResponseAsync { partialResult, done ->
                if (!isClosedForSend) {
                    trySend(partialResult)
                    if (done) close()
                }
            }
        } catch (e: Exception) {
            close(e)
        }

        awaitClose()
    }

    // ─── One-shot Generation (for API server) ─────────────────────────────────

    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        val inference = llmInference
            ?: return@withContext Result.failure(Exception("No model loaded"))
        try {
            val session = LlmInferenceSession.createFromLlmInference(
                inference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(0.7f).setTopK(40).build()
            )
            val fullPrompt = buildGemmaPrompt(systemPrompt, prompt)
            session.addQueryChunk(fullPrompt)
            val result = session.generateResponse()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Code Generation Helper ───────────────────────────────────────────────

    suspend fun generateCode(
        instruction: String,
        existingCode: String? = null,
        language: String = "auto"
    ): Result<String> {
        val system = """You are an expert software engineer. 
When asked to generate or modify code:
1. Return ONLY the complete, working code
2. No explanations outside code comments
3. Use modern best practices
4. Handle edge cases
Language context: $language"""

        val prompt = if (existingCode != null) {
            "Modify this code:\n```\n$existingCode\n```\n\nInstruction: $instruction"
        } else {
            "Generate code for: $instruction"
        }
        return generate(prompt, system, maxTokens = 4096)
    }

    // ─── Project Generation ───────────────────────────────────────────────────

    suspend fun generateProject(description: String): Result<String> {
        val system = """You are an expert software architect and developer.
When asked to create a project, respond with a JSON object containing the file structure:
{
  "projectName": "name",
  "files": [
    {"path": "relative/path/file.ext", "content": "full file content here"},
    ...
  ]
}
Create complete, working, production-ready code. Include all necessary files."""

        return generate(description, system, maxTokens = 8192)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun buildGemmaPrompt(system: String, user: String): String {
        val sb = StringBuilder()
        if (system.isNotBlank()) {
            sb.append("<start_of_turn>system\n$system<end_of_turn>\n")
        }
        sb.append("<start_of_turn>user\n$user<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    private fun loadBitmap(uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bitmap", e); null
    }

    fun close() {
        llmInference?.close()
        llmInference = null
        currentModelId = null
    }
}
