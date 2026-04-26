package com.laiserdev.localllm.data.repository

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

class LLMRepository(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var currentModelId: String? = null
    private val TAG = "LLMRepository"

    suspend fun loadModel(modelId: String, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (currentModelId == modelId && llmInference != null) return@withContext Result.success(Unit)

                // Primary location: filesDir/models (where ModelDownloadService saves)
                val modelFile = File(context.filesDir, "models/$fileName")
                if (!modelFile.exists()) {
                    return@withContext Result.failure(
                        Exception("Model file not found. Please download it first from the Models tab.")
                    )
                }

                // MediaPipe requires the file to be readable. filesDir is app-private
                // but MediaPipe reads it directly via the path — this works fine as long
                // as we pass the absolute path from the same app context.
                llmInference?.close()
                llmInference = null

                Log.d(TAG, "Loading model from: ${modelFile.absolutePath} (${modelFile.length() / 1_000_000}MB)")

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(4096)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                currentModelId = modelId
                Log.d(TAG, "✅ Model loaded: $fileName")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Load failed", e)
                Result.failure(Exception("Failed to load model: ${e.message?.take(200) ?: "Unknown error"}"))
            }
        }

    fun isLoaded() = llmInference != null
    fun currentModel() = currentModelId

    // Streaming: MediaPipe 0.10.14 returns full response at once.
    // We chunk it into word-sized tokens with a small delay for a smooth streaming UX.
    fun generateStream(prompt: String, systemPrompt: String = ""): Flow<String> = flow {
        val inference = llmInference ?: throw Exception("No model loaded")
        val full = withContext(Dispatchers.IO) {
            inference.generateResponse(buildPrompt(systemPrompt, prompt))
        }
        // Emit word by word with a short delay for visual streaming effect
        val words = full.split(Regex("(?<=\\s)|(?=\\s)"))
        for (word in words) {
            emit(word)
            if (word.isNotBlank()) kotlinx.coroutines.delay(18L)
        }
    }

    suspend fun generate(prompt: String, systemPrompt: String = "", maxTokens: Int = 1024): Result<String> =
        withContext(Dispatchers.IO) {
            val inference = llmInference ?: return@withContext Result.failure(Exception("No model loaded"))
            try {
                Result.success(inference.generateResponse(buildPrompt(systemPrompt, prompt)))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun generateCode(instruction: String, existingCode: String? = null, language: String = "auto"): Result<String> {
        val system = "You are an expert software engineer. Return ONLY complete working code. Language: $language"
        val prompt = if (existingCode != null) "Modify:\n```\n$existingCode\n```\nInstruction: $instruction"
        else "Generate: $instruction"
        return generate(prompt, system, 4096)
    }

    suspend fun generateProject(description: String): Result<String> =
        generate(description,
            "Respond ONLY with JSON: {\"projectName\":\"name\",\"files\":[{\"path\":\"path\",\"content\":\"content\"}]}",
            8192)

    private fun buildPrompt(system: String, user: String) = buildString {
        if (system.isNotBlank()) append("<start_of_turn>system\n$system<end_of_turn>\n")
        append("<start_of_turn>user\n$user<end_of_turn>\n<start_of_turn>model\n")
    }

    fun close() { llmInference?.close(); llmInference = null; currentModelId = null }
}
