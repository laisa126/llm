package com.laiserdev.localllm.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import java.io.File

/**
 * Global registry so ToolEngine can grab the live WebView from PreviewScreen
 * without needing a permission or MediaProjection.
 * WebView.draw() into a Bitmap is always allowed — it's our own view.
 */
object WebViewRegistry {
    @Volatile
    var activeWebView: WebView? = null

    /**
     * Captures the current WebView contents to a PNG file.
     * Returns the file path on success, or an error string.
     * Must be called on the main thread — caller is responsible.
     */
    fun captureToFile(outputPath: String): Result<String> {
        val wv = activeWebView
            ?: return Result.failure(Exception("No preview open. Open a file in the Preview tab first."))
        return try {
            val w = wv.width.takeIf { it > 0 } ?: 800
            val h = wv.height.takeIf { it > 0 } ?: 600
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            wv.draw(canvas)
            val file = File(outputPath).also { it.parentFile?.mkdirs() }
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap.recycle()
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
