package com.laiserdev.localllm.data.repository

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.laiserdev.localllm.LocalLLMApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    companion object {
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_FILE_NAME = "file_name"
        const val ACTION_CANCEL = "cancel_download"

        private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
        val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

        private val _downloadStatus = MutableStateFlow<Map<String, String>>(emptyMap())
        val downloadStatus: StateFlow<Map<String, String>> = _downloadStatus
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            scope.coroutineContext.cancelChildren()
            stopSelf()
            return START_NOT_STICKY
        }

        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
        val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: modelId
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return START_NOT_STICKY

        startForeground(modelId.hashCode(), buildNotification(modelName, 0))
        downloadModel(modelId, modelName, downloadUrl, fileName)
        return START_STICKY
    }

    private fun downloadModel(
        modelId: String, modelName: String,
        downloadUrl: String, fileName: String
    ) {
        scope.launch {
            try {
                updateStatus(modelId, "downloading")
                val modelsDir = File(filesDir, "models").also { it.mkdirs() }
                val outputFile = File(modelsDir, fileName)
                val tempFile = File(modelsDir, "$fileName.tmp")

                // HuggingFace redirects across CDN domains (HTTPS→HTTPS).
                // HttpURLConnection won't follow cross-host redirects automatically,
                // so we follow up to 5 hops manually.
                var currentUrl = downloadUrl
                var finalConn: HttpURLConnection? = null
                for (i in 0..5) {
                    val conn = URL(currentUrl).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 60_000
                    conn.setRequestProperty("User-Agent", "LocalLLM-Android/1.0")
                    conn.setRequestProperty("Accept", "*/*")
                    conn.connect()
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        currentUrl = if (location.startsWith("http")) location
                            else URL(URL(currentUrl), location).toString()
                    } else {
                        finalConn = conn
                        break
                    }
                }

                val conn = finalConn ?: throw Exception("Too many redirects")

                // Resume support: if temp file exists, request bytes from where we left off
                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
                if (existingBytes > 0) {
                    conn.disconnect()
                    // Re-open connection with Range header
                    val resumeConn = URL(currentUrl).openConnection() as HttpURLConnection
                    resumeConn.instanceFollowRedirects = true
                    resumeConn.connectTimeout = 30_000
                    resumeConn.readTimeout = 60_000
                    resumeConn.setRequestProperty("User-Agent", "LocalLLM-Android/1.0")
                    resumeConn.setRequestProperty("Range", "bytes=$existingBytes-")
                    resumeConn.connect()
                    val resumeCode = resumeConn.responseCode
                    if (resumeCode == 206) { // Partial content — resume works
                        val totalBytes = existingBytes + resumeConn.contentLengthLong
                        var downloadedBytes = existingBytes
                        resumeConn.inputStream.use { input ->
                            tempFile.outputStream().also { it.channel.position(existingBytes) }.use { output ->
                                val buffer = ByteArray(32_768)
                                var bytes: Int
                                while (input.read(buffer).also { bytes = it } != -1) {
                                    output.write(buffer, 0, bytes)
                                    downloadedBytes += bytes
                                    val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) else 0f
                                    updateProgress(modelId, progress)
                                    updateNotification(modelName, (progress * 100).toInt())
                                }
                            }
                        }
                        tempFile.renameTo(outputFile)
                        updateProgress(modelId, 1f)
                        updateStatus(modelId, "ready")
                        notifyComplete(modelName)
                        return@launch
                    }
                    // Server doesn't support resume — fall through to full download
                    resumeConn.disconnect()
                    tempFile.delete()
                }

                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP $responseCode")
                }

                val totalBytes = conn.contentLengthLong
                var downloadedBytes = 0L

                conn.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(32_768)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes
                            val progress = if (totalBytes > 0)
                                (downloadedBytes.toFloat() / totalBytes) else 0f
                            updateProgress(modelId, progress)
                            updateNotification(modelName, (progress * 100).toInt())
                        }
                    }
                }

                tempFile.renameTo(outputFile)
                updateProgress(modelId, 1f)
                updateStatus(modelId, "ready")
                notifyComplete(modelName)
            } catch (e: Exception) {
                // Strip raw URLs from error message — confusing in UI
                val msg = (e.message ?: "Unknown error")
                    .replace(Regex("https?://\\S+"), "[url]")
                    .take(120)
                updateStatus(modelId, "error:$msg")
            } finally {
                stopSelf()
            }
        }
    }

    private fun updateProgress(modelId: String, progress: Float) {
        _downloadProgress.value = _downloadProgress.value + (modelId to progress)
    }

    private fun updateStatus(modelId: String, status: String) {
        _downloadStatus.value = _downloadStatus.value + (modelId to status)
    }

    private fun buildNotification(name: String, progress: Int) =
        NotificationCompat.Builder(this, LocalLLMApp.CHANNEL_DOWNLOAD)
            .setContentTitle("Downloading $name")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

    private fun updateNotification(name: String, progress: Int) {
        notificationManager.notify(name.hashCode(), buildNotification(name, progress))
    }

    private fun notifyComplete(name: String) {
        val n = NotificationCompat.Builder(this, LocalLLMApp.CHANNEL_DOWNLOAD)
            .setContentTitle("$name ready")
            .setContentText("Model downloaded successfully")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((name + "_done").hashCode(), n)
    }
}
