package com.laiserdev.localllm.ui.screens.models

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.laiserdev.localllm.data.repository.ModelBootstrap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.data.model.LLMModel
import com.laiserdev.localllm.data.model.ModelStatus
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*

@Composable
fun ModelsScreen(vm: MainViewModel, onOpenDrawer: () -> Unit = {}) {
    val models by vm.models.collectAsState()
    val settings by vm.settings.collectAsState()
    val loadingState by vm.modelLoadingState.collectAsState()

    Column(Modifier.fillMaxSize().background(BgDeep)) {
        com.laiserdev.localllm.ui.AppTopBar("AI Models", onOpenDrawer)

        // HF token warning banner — only for downloadable models (bundled model doesn't need it)
        val hasDownloadableModels = models.any {
            it.id != ModelBootstrap.BUNDLED_MODEL_ID &&
            it.status != ModelStatus.READY && it.status != ModelStatus.LOADED
        }
        if (settings.hfToken.isBlank() && hasDownloadableModels) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1500))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = WarnYellow)
                    Spacer(Modifier.width(6.dp))
                    Text("Token needed to download additional models",
                        color = WarnYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Gemma 3 4B and 3n E4B require a HuggingFace token.\n" +
                    "The built-in Gemma 3 1B ⭐ already works — no token needed.",
                    color = TextSecond, fontSize = 11.sp, lineHeight = 15.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Accept license button
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://huggingface.co/litert-community/Gemma3-4B-IT")
                            )
                            context.startActivity(intent)
                        },
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, WarnYellow.copy(0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnYellow),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Accept License", fontSize = 10.sp)
                    }
                    // Go to settings
                    Button(
                        onClick = { /* navigate handled by drawer */ },
                        colors = ButtonDefaults.buttonColors(containerColor = WarnYellow, contentColor = BgDeep),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Add Token in Settings", fontSize = 10.sp)
                    }
                }
            }
            HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
        }
        if (loadingState != null) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF1A2A1A)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), color = AccentGreen, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(loadingState!!, color = AccentGreen, fontSize = 13.sp)
            }
        }

        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(models) { model ->
                ModelCard(
                    model = model,
                    isActive = model.id == settings.activeModelId,
                    isDownloaded = vm.isModelDownloaded(model),
                    onDownload = { vm.downloadModel(model) },
                    onLoad = { vm.loadModel(model) }
                )
            }

            item {
                HardwareInfoCard()
            }
        }
    }
}

@Composable
fun ModelCard(
    model: LLMModel, isActive: Boolean, isDownloaded: Boolean,
    onDownload: () -> Unit, onLoad: () -> Unit
) {
    val borderColor = when {
        model.status == ModelStatus.LOADED -> AccentGreen
        isActive -> AccentBlue
        else -> BgBorder
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(if (isActive || model.status == ModelStatus.LOADED) 1.dp else 0.5.dp, borderColor), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (model.id == com.laiserdev.localllm.data.repository.ModelBootstrap.BUNDLED_MODEL_ID) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .background(Color(0xFF0D2A1A), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, AccentGreen.copy(0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Built-in", color = AccentGreen, fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        if (model.status == ModelStatus.LOADED) {
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.background(Color(0xFF1A3A2A), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("LOADED", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(model.description, color = TextSecond, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Text("${model.sizeGb}GB", color = TextMuted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Capabilities chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CapabilityChip("≥${model.minRamGb}GB RAM", Icons.Default.Memory)
                if (model.supportsVision) CapabilityChip("Vision", Icons.Default.Visibility)
                if (model.supportsCode) CapabilityChip("Code", Icons.Default.Code)
            }

            // Download progress
            if (model.status == ModelStatus.DOWNLOADING) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { model.downloadProgress },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = AccentGreen, trackColor = BgElevated
                )
                Spacer(Modifier.height(4.dp))
                val pct = (model.downloadProgress * 100).toInt()
                val downloaded = (model.downloadProgress * model.sizeGb * 1024).toInt()
                val total = (model.sizeGb * 1024).toInt()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$pct%  ·  ${downloaded}MB / ${total}MB",
                        color = AccentGreen, fontSize = 11.sp)
                    if (model.downloadProgress > 0 && model.downloadProgress < 1f) {
                        Text("Downloading...", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // Error
            if (model.status == ModelStatus.ERROR && model.errorMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text("Error: ${model.errorMessage}", color = ErrorRed, fontSize = 11.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Action button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                when {
                    model.status == ModelStatus.DOWNLOADING -> {
                        OutlinedButton(onClick = {},
                            border = BorderStroke(0.5.dp, BgBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)) {
                            Text("Downloading...", fontSize = 12.sp)
                        }
                    }
                    model.status == ModelStatus.LOADING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = AccentBlue, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Loading...", color = AccentBlue, fontSize = 12.sp)
                        }
                    }
                    model.status == ModelStatus.LOADED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = AccentGreen)
                            Spacer(Modifier.width(6.dp))
                            Text("Active", color = AccentGreen, fontSize = 12.sp)
                        }
                    }
                    isDownloaded -> {
                        Button(onClick = onLoad,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Load Model", fontSize = 12.sp)
                        }
                    }
                    else -> {
                        Button(onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.Download, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilityChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.background(BgElevated, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(11.dp), tint = TextMuted)
        Spacer(Modifier.width(3.dp))
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
fun HardwareInfoCard() {
    val runtime = Runtime.getRuntime()
    val maxMem = runtime.maxMemory() / (1024 * 1024)
    val totalMem = runtime.totalMemory() / (1024 * 1024)
    val freeMem = runtime.freeMemory() / (1024 * 1024)
    val usedMem = totalMem - freeMem

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        border = BorderStroke(0.5.dp, BgBorder)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Device Info", color = TextSecond, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoRow("JVM Max Memory", "${maxMem}MB")
            InfoRow("JVM Used Memory", "${usedMem}MB")
            InfoRow("Android API", android.os.Build.VERSION.SDK_INT.toString())
            InfoRow("CPU Cores", Runtime.getRuntime().availableProcessors().toString())
            InfoRow("CPU ABI", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecond, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp)
    }
}
