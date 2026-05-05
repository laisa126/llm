package com.laiserdev.localllm.ui.screens.preview

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.util.WebViewRegistry
import com.laiserdev.localllm.ui.theme.*
import java.io.File

@Composable
fun PreviewScreen(vm: MainViewModel, onOpenDrawer: () -> Unit = {}) {
    val activeFilePath by vm.activeFilePath.collectAsState()
    val openFiles by vm.openFiles.collectAsState()
    val activeProject by vm.activeProject.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { WebViewRegistry.activeWebView = null } }
    var pageTitle by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Pick HTML content from active file or search open files for any html
    val htmlFile = remember(activeFilePath, openFiles) {
        val path = activeFilePath
        if (path != null && path.endsWith(".html", ignoreCase = true)) {
            openFiles.firstOrNull { it.first == path }
        } else {
            openFiles.firstOrNull { it.first.endsWith(".html", ignoreCase = true) }
        }
    }

    Column(Modifier.fillMaxSize().background(BgDeep)) {

        // ── Header ─────────────────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().background(BgSurface)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer, Modifier.size(40.dp)) {
                    Icon(Icons.Default.Menu, null, Modifier.size(20.dp), tint = TextSecond)
                }
                Text(
                    "Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Back / Forward
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, "Back",
                            Modifier.size(16.dp),
                            tint = if (canGoBack) TextSecond else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward, "Forward",
                            Modifier.size(16.dp),
                            tint = if (canGoForward) TextSecond else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, "Reload", Modifier.size(16.dp), tint = TextSecond)
                    }
                }
            }

            // URL / file bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
                    .background(BgElevated, RoundedCornerShape(6.dp))
                    .border(0.5.dp, BgBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(12.dp), color = AccentBlue, strokeWidth = 1.5.dp)
                } else {
                    Icon(
                        if (htmlFile != null) Icons.Default.Html else Icons.Default.Language,
                        null, Modifier.size(12.dp),
                        tint = if (htmlFile != null) Color(0xFFE34F26) else TextMuted
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    htmlFile?.first?.let { File(it).name } ?: pageTitle.ifBlank { "No HTML file open" },
                    color = if (htmlFile != null) TextPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }

            // Loading progress bar
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentBlue,
                    trackColor = BgElevated
                )
            }
        }

        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        // ── WebView or empty state ─────────────────────────────────────────────
        if (htmlFile != null) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                pageTitle = view?.title ?: ""
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                        }
                        webViewRef = this
                        WebViewRegistry.activeWebView = this
                    }
                },
                update = { webView ->
                    val projectPath = activeProject?.path ?: ""
                    webView.loadDataWithBaseURL(
                        "file://$projectPath/",
                        htmlFile.second,
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            // Empty state
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Html, null, Modifier.size(52.dp), tint = TextMuted)
                    Text("No HTML file open", color = TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Open a .html file in the Editor to preview it here.",
                        color = TextMuted, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    // Quick example projects
                    Text("Try asking the agent:", color = TextSecond, fontSize = 12.sp)
                    listOf(
                        "Build a to-do app in HTML/CSS/JS",
                        "Create a landing page",
                        "Make a calculator app"
                    ).forEach { suggestion ->
                        Box(
                            Modifier
                                .background(BgElevated, RoundedCornerShape(8.dp))
                                .border(0.5.dp, BgBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(suggestion, color = TextSecond, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Keep PreviewPanel for use in EditorScreen split view
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
                    Text("• ${File(activeFilePath).name}", color = TextMuted, fontSize = 11.sp)
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
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) { isLoading = true }
                            override fun onPageFinished(view: WebView?, url: String?) { isLoading = false }
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
                        WebViewRegistry.activeWebView = this
                    }
                },
                update = { webView ->
                    val projectPath = vm.activeProject.value?.path ?: ""
                    webView.loadDataWithBaseURL(
                        "file://$projectPath/",
                        htmlContent, "text/html", "UTF-8", null
                    )
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Html, null, Modifier.size(40.dp), tint = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Open an .html file to preview it here", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
