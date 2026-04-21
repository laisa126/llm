package com.laiserdev.localllm.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
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
                    return@withContext Result.success(Unit)
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
            val fullPrompt = buildGemmaPrompt(systemPrompt, prompt)
            inference.generateResponseAsync(fullPrompt) { partialResult, done ->
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

    // ─── One-shot Generation ─────────────────────────────────────────────────

    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        val inference = llmInference
            ?: return@withContext Result.failure(Exception("No model loaded"))
        try {
            val fullPrompt = buildGemmaPrompt(systemPrompt, prompt)
            val result = inference.generateResponse(fullPrompt)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Code Generation ─────────────────────────────────────────────────────

    suspend fun generateCode(
        instruction: String,
        existingCode: String? = null,
        language: String = "auto"
    ): Result<String> {
        val system = "You are an expert software engineer. Return ONLY complete working code. Language: $language"
        val prompt = if (existingCode != null)
            "Modify this code:\n```\n$existingCode\n```\n\nInstruction: $instruction"
        else "Generate code for: $instruction"
        return generate(prompt, system, maxTokens = 4096)
    }

    // ─── Project Generation ──────────────────────────────────────────────────

    suspend fun generateProject(description: String): Result<String> {
        val system = """You are an expert software architect.
Respond with ONLY a JSON object:
{"projectName":"name","files":[{"path":"relative/path","content":"full content"}]}
Create complete, working, production-ready code."""
        return generate(description, system, maxTokens = 8192)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildGemmaPrompt(system: String, user: String): String {
        val sb = StringBuilder()
        if (system.isNotBlank()) sb.append("<start_of_turn>system\n$system<end_of_turn>\n")
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
    } catch (e: Exception) { null }

    fun close() {
        llmInference?.close()
        llmInference = null
        currentModelId = null
    }
}
