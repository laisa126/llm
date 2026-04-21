package com.laiserdev.localllm.ui.screens.preview

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*
import java.io.File

@Composable
fun PreviewPanel(vm: MainViewModel, modifier: Modifier = Modifier) {
    val activeFilePath by vm.activeFilePath.collectAsState()
    val openFiles by vm.openFiles.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val htmlContent = remember(activeFilePath, openFiles) {
        val path = activeFilePath ?: return@remember null
        if (!path.endsWith(".html", ignoreCase = true)) return@remember null
        openFiles.firstOrNull { it.first == path }?.second
    }

    Column(modifier.background(BgDeep)) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().background(BgSurface).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Preview, null, Modifier.size(14.dp), tint = AccentBlue)
                Spacer(Modifier.width(6.dp))
                Text("Live Preview", color = TextSecond, fontSize = 12.sp)
                if (activeFilePath != null) {
                    Spacer(Modifier.width(6.dp))
                    Text("• ${File(activeFilePath!!).name}", color = TextMuted, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = AccentBlue, strokeWidth = 2.dp)
                }
                IconButton(onClick = { webViewRef?.reload() }, Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, "Reload", tint = TextSecond, modifier = Modifier.size(14.dp))
                }
            }
        }
        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        if (htmlContent != null) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        webViewRef = this
                    }
                },
                update = { webView ->
                    val projectPath = vm.activeProject.value?.path ?: ""
                    webView.loadDataWithBaseURL(
                        "file://$projectPath/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Html, null, Modifier.size(40.dp), tint = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Open an .html file to preview it here",
                        color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
