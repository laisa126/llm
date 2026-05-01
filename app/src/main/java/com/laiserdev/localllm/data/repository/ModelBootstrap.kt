package com.laiserdev.localllm.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies the bundled Gemma 3 1B model from assets → filesDir on first launch.
 * Emits progress 0.0..1.0 then 1.0 when done.
 *
 * The model is embedded in the APK at:
 *   assets/models/gemma3-1b-int4.task  (placed there by GitHub Actions)
 *
 * It is copied to:
 *   filesDir/models/gemma3-1b-int4.task  (where LLMRepository reads it)
 */
object ModelBootstrap {

    const val BUNDLED_MODEL_ID   = "gemma3-1b"
    const val BUNDLED_MODEL_FILE = "gemma3-1b-q8.task"
    const val BUNDLED_ASSET_PATH = "models/$BUNDLED_MODEL_FILE"

    private val TAG = "ModelBootstrap"

    /** Returns true if the bundled model asset is present in the APK. */
    fun isBundled(context: Context): Boolean = try {
        context.assets.open(BUNDLED_ASSET_PATH).use { true }
    } catch (_: Exception) { false }

    /** Returns true if the model has already been extracted to filesDir. */
    fun isExtracted(context: Context): Boolean =
        File(context.filesDir, "models/$BUNDLED_MODEL_FILE").exists()

    /**
     * Extracts the bundled model from assets to filesDir.
     * Emits Float progress 0.0..1.0.
     * Throws on failure.
     */
    fun extract(context: Context): Flow<Float> = channelFlow {
        send(0f)
        val destDir  = File(context.filesDir, "models").also { it.mkdirs() }
        val destFile = File(destDir, BUNDLED_MODEL_FILE)
        val tmpFile  = File(destDir, "$BUNDLED_MODEL_FILE.tmp")

        withContext(Dispatchers.IO) {
            val assetManager = context.assets
            val totalBytes = try {
                assetManager.openFd(BUNDLED_ASSET_PATH).use { it.length }
            } catch (_: Exception) { -1L }

            var written = 0L

            assetManager.open(BUNDLED_ASSET_PATH).use { input ->
                tmpFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024) // 64KB chunks
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        written += bytes
                        if (totalBytes > 0) {
                            val progress = written.toFloat() / totalBytes
                            channel.trySend(progress.coerceIn(0f, 0.99f))
                        }
                    }
                }
            }
            tmpFile.renameTo(destFile)
            Log.d(TAG, "✅ Bundled model extracted: ${destFile.length() / 1_000_000}MB")
        }
        send(1f)
    }
}
