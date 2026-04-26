package com.laiserdev.localllm.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.laiserdev.localllm.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "localllm_settings")

class SettingsManager(private val context: Context) {

    private object Keys {
        val SERVER_PORT = intPreferencesKey("server_port")
        val SERVER_ENABLED = booleanPreferencesKey("server_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = intPreferencesKey("font_size")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serverPort = prefs[Keys.SERVER_PORT] ?: 8080,
            serverEnabled = prefs[Keys.SERVER_ENABLED] ?: false,
            activeModelId = prefs[Keys.ACTIVE_MODEL_ID] ?: "gemma3-1b",
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 2048,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
            systemPrompt = prefs[Keys.SYSTEM_PROMPT]
                ?: "You are an expert software engineer AI assistant.",
            theme = prefs[Keys.THEME] ?: "dark",
            fontSize = prefs[Keys.FONT_SIZE] ?: 14
        )
    }

    suspend fun update(block: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setActiveModel(id: String) = update { it[Keys.ACTIVE_MODEL_ID] = id }
    suspend fun setServerEnabled(enabled: Boolean) = update { it[Keys.SERVER_ENABLED] = enabled }
    suspend fun setOnboardingDone() = update { it[Keys.ONBOARDING_DONE] = true }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    suspend fun setSystemPrompt(prompt: String) = update { it[Keys.SYSTEM_PROMPT] = prompt }
    suspend fun setTemperature(t: Float) = update { it[Keys.TEMPERATURE] = t }
    suspend fun setMaxTokens(n: Int) = update { it[Keys.MAX_TOKENS] = n }
    suspend fun setTheme(theme: String) = update { it[Keys.THEME] = theme }
    suspend fun setFontSize(size: Int) = update { it[Keys.FONT_SIZE] = size }
}
