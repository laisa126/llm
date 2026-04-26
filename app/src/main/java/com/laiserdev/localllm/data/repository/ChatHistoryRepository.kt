package com.laiserdev.localllm.data.repository

import android.content.Context
import com.laiserdev.localllm.data.model.ChatMessage
import com.laiserdev.localllm.data.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists chat history to a JSON file in internal storage.
 * Each session is one file: chat_YYYYMMDD_HHMMSS.json
 * We keep the last 20 sessions max.
 */
class ChatHistoryRepository(private val context: Context) {

    private val historyDir = File(context.filesDir, "chat_history").also { it.mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val maxSessions = 20
    private val currentSessionFile: File

    init {
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        currentSessionFile = File(historyDir, "chat_$ts.json")
    }

    // ── Serializable wrapper (ChatMessage has non-serializable UUID) ──────────

    @Serializable
    private data class StoredMessage(
        val id: String,
        val role: String,
        val content: String,
        val imageUri: String? = null,
        val timestamp: Long,
        val tokensPerSecond: Float = 0f
    )

    private fun ChatMessage.toStored() = StoredMessage(
        id = id, role = role.name, content = content,
        imageUri = imageUri, timestamp = timestamp, tokensPerSecond = tokensPerSecond
    )

    private fun StoredMessage.toMessage() = ChatMessage(
        id = id,
        role = MessageRole.valueOf(role),
        content = content,
        imageUri = imageUri,
        timestamp = timestamp,
        tokensPerSecond = tokensPerSecond
    )

    // ── Save current session ──────────────────────────────────────────────────

    suspend fun saveSession(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        try {
            val stored = messages
                .filter { !it.isStreaming }
                .map { it.toStored() }
            currentSessionFile.writeText(json.encodeToString(stored))
            pruneOldSessions()
        } catch (_: Exception) {}
    }

    // ── Load most recent session ──────────────────────────────────────────────

    suspend fun loadLastSession(): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val files = historyDir.listFiles { f -> f.extension == "json" }
                ?.sortedByDescending { it.lastModified() }
                ?: return@withContext emptyList()
            // Skip the current session file (just created, empty)
            val toLoad = files.firstOrNull { it.name != currentSessionFile.name }
                ?: return@withContext emptyList()
            val stored = json.decodeFromString<List<StoredMessage>>(toLoad.readText())
            stored.map { it.toMessage() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── List all sessions ─────────────────────────────────────────────────────

    suspend fun listSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        historyDir.listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { f ->
                try {
                    val stored = json.decodeFromString<List<StoredMessage>>(f.readText())
                    if (stored.isEmpty()) return@mapNotNull null
                    ChatSession(
                        fileName = f.name,
                        title = stored.firstOrNull { it.role == "USER" }?.content
                            ?.take(60) ?: "Chat",
                        messageCount = stored.size,
                        lastTimestamp = stored.lastOrNull()?.timestamp ?: f.lastModified()
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()
    }

    // ── Load a specific session ───────────────────────────────────────────────

    suspend fun loadSession(fileName: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val f = File(historyDir, fileName)
            val stored = json.decodeFromString<List<StoredMessage>>(f.readText())
            stored.map { it.toMessage() }
        } catch (_: Exception) { emptyList() }
    }

    // ── Delete a session ──────────────────────────────────────────────────────

    suspend fun deleteSession(fileName: String) = withContext(Dispatchers.IO) {
        File(historyDir, fileName).delete()
    }

    private fun pruneOldSessions() {
        val files = historyDir.listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: return
        files.drop(maxSessions).forEach { it.delete() }
    }
}

data class ChatSession(
    val fileName: String,
    val title: String,
    val messageCount: Int,
    val lastTimestamp: Long
)
