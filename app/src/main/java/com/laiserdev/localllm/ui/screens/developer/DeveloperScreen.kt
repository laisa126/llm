package com.laiserdev.localllm.ui.screens.developer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.BuildConfig
import com.laiserdev.localllm.server.LLMServerService
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*
import java.net.NetworkInterface

@Composable
fun DeveloperScreen(vm: MainViewModel) {
    val serverRunning by vm.serverRunning.collectAsState()
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val deviceIp = remember { getLocalIpAddress() }
    val port = BuildConfig.API_PORT

    Column(Modifier.fillMaxSize().background(BgDeep).verticalScroll(rememberScrollState())) {
        // Header
        Column(Modifier.fillMaxWidth().background(BgSurface).padding(16.dp)) {
            Text("Developer API", style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("Integrate LocalLLM into your apps", color = TextSecond, fontSize = 13.sp)
        }
        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Server toggle ──────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = BgSurface),
                border = BorderStroke(0.5.dp, if (serverRunning) AccentGreen else BgBorder),
                shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("API Server", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                if (serverRunning) "Running on port $port" else "Start to expose local API",
                                color = if (serverRunning) AccentGreen else TextSecond, fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = serverRunning,
                            onCheckedChange = { vm.toggleServer(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgDeep, checkedTrackColor = AccentGreen
                            )
                        )
                    }

                    if (serverRunning && deviceIp != null) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("Endpoints", color = TextSecond, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            "Local" to "http://localhost:$port",
                            "Network" to "http://$deviceIp:$port"
                        ).forEach { (label, url) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(label, color = TextMuted, fontSize = 10.sp)
                                    Text(url, color = AccentBlue, fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace)
                                }
                                IconButton(onClick = { copyToClipboard(context, url) }, Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = TextSecond)
                                }
                            }
                        }
                    }
                }
            }

            // ── Get API keys ───────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = BgSurface),
                border = BorderStroke(0.5.dp, BgBorder), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("API Keys", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Get a free API key to authenticate your requests.",
                        color = TextSecond, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { /* Open dashboard URL */ },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get API Key at dashboard")
                    }
                }
            }

            // ── API Docs ───────────────────────────────────────────────────────
            Text("API Reference", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            ApiEndpointCard(
                method = "GET", path = "/health",
                description = "Check if server is running and model is loaded",
                auth = false,
                responseExample = """{"status":"ok","modelLoaded":true}"""
            )
            ApiEndpointCard(
                method = "POST", path = "/v1/chat",
                description = "Send a prompt, get a completion back",
                auth = true,
                requestExample = """{"prompt":"Write hello world in Python","maxTokens":512}""",
                responseExample = """{"id":"uuid","content":"print('Hello, World!')","model":"gemma3-4b","latencyMs":1240}"""
            )
            ApiEndpointCard(
                method = "POST", path = "/v1/code",
                description = "Code generation — system prompt is pre-set for code",
                auth = true,
                requestExample = """{"prompt":"Build a REST API with Express.js","mode":"code"}""",
                responseExample = """{"id":"uuid","content":"const express = require('express')...","model":"gemma3-4b"}"""
            )
            ApiEndpointCard(
                method = "GET", path = "/v1/models",
                description = "Get info about currently loaded model",
                auth = true,
                responseExample = """{"loaded":"gemma3-4b","capabilities":["chat","code"]}"""
            )

            // ── SDK example ────────────────────────────────────────────────────
            Text("Quick Start", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            CodeBlock("""// JavaScript / Node.js
const res = await fetch('http://localhost:8080/v1/chat', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': 'your_api_key_here'
  },
  body: JSON.stringify({ prompt: 'Hello!' })
});
const data = await res.json();
console.log(data.content);""")

            CodeBlock("""# Python
import requests

res = requests.post('http://localhost:8080/v1/chat',
  headers={'X-API-Key': 'your_key'},
  json={'prompt': 'Explain async/await'}
)
print(res.json()['content'])""")

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ApiEndpointCard(
    method: String, path: String, description: String, auth: Boolean,
    requestExample: String? = null, responseExample: String? = null
) {
    val methodColor = when (method) {
        "GET"    -> Color(0xFF3FB950)
        "POST"   -> Color(0xFF58A6FF)
        "DELETE" -> Color(0xFFF85149)
        else     -> TextSecond
    }
    Card(colors = CardDefaults.cardColors(containerColor = BgSurface),
        border = BorderStroke(0.5.dp, BgBorder), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(methodColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(method, color = methodColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.width(8.dp))
                Text(path, color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                if (auth) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Lock, null, Modifier.size(12.dp), tint = AccentOrange)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(description, color = TextSecond, fontSize = 12.sp)
            if (requestExample != null) {
                Spacer(Modifier.height(6.dp))
                Text("Request", color = TextMuted, fontSize = 10.sp)
                CodeSnippet(requestExample)
            }
            if (responseExample != null) {
                Spacer(Modifier.height(4.dp))
                Text("Response", color = TextMuted, fontSize = 10.sp)
                CodeSnippet(responseExample)
            }
        }
    }
}

@Composable
fun CodeSnippet(code: String) {
    Box(Modifier.fillMaxWidth().background(BgDeep, RoundedCornerShape(4.dp)).padding(8.dp)) {
        Text(code, color = AccentGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
    }
}

@Composable
fun CodeBlock(code: String) {
    val context = LocalContext.current
    Box(Modifier.fillMaxWidth().background(BgSurface, RoundedCornerShape(8.dp))
        .border(BorderStroke(0.5.dp, BgBorder), RoundedCornerShape(8.dp))) {
        Column {
            Row(Modifier.fillMaxWidth().background(BgElevated, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Code", color = TextMuted, fontSize = 10.sp)
                Icon(Icons.Default.ContentCopy, "Copy",
                    Modifier.size(14.dp).clickable { copyToClipboard(context, code) }, tint = TextSecond)
            }
            Text(code, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp, modifier = Modifier.padding(12.dp))
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("LocalLLM", text))
}

private fun getLocalIpAddress(): String? = try {
    NetworkInterface.getNetworkInterfaces()?.toList()
        ?.flatMap { it.inetAddresses.toList() }
        ?.firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
        ?.hostAddress
} catch (e: Exception) { null }
