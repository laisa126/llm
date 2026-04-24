package com.laiserdev.localllm.data.repository

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
                val modelFile = File(context.filesDir, "models/$fileName")
                if (!modelFile.exists()) return@withContext Result.failure(Exception("Model not downloaded: $fileName"))
                llmInference?.close(); llmInference = null
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(4096)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                currentModelId = modelId
                Log.d(TAG, "✅ Model loaded: $fileName")
                Result.success(Unit)
            } catch (e: Exception) { Log.e(TAG, "❌ Failed", e); Result.failure(e) }
        }

    fun isLoaded() = llmInference != null
    fun currentModel() = currentModelId

    // Streaming: run blocking generateResponse on IO thread, emit full result as single token
    // MediaPipe 0.10.14 doesn't expose token streaming - full response comes at once
    fun generateStream(prompt: String, systemPrompt: String = ""): Flow<String> = flow {
        val inference = llmInference ?: throw Exception("No model loaded")
        val result = inference.generateResponse(buildPrompt(systemPrompt, prompt))
        emit(result)
    }.flowOn(Dispatchers.IO)

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
