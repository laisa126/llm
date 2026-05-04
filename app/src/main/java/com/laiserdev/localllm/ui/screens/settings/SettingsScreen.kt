package com.laiserdev.localllm.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.BuildConfig
import com.laiserdev.localllm.ui.AppTopBar
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenDrawer: () -> Unit = {}) {
    val settings by vm.settings.collectAsState()
    var systemPrompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var temperature   by remember(settings.temperature)  { mutableFloatStateOf(settings.temperature) }
    var maxTokens     by remember(settings.maxTokens)    { mutableIntStateOf(settings.maxTokens) }
    var fontSize      by remember(settings.fontSize)     { mutableIntStateOf(settings.fontSize) }

    Column(
        Modifier.fillMaxSize().background(BgDeep)
            .verticalScroll(rememberScrollState())
    ) {
        AppTopBar("Settings", onOpenDrawer)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // ── Model section ─────────────────────────────────────────────────
            SettingSection("Model", Icons.Outlined.Psychology) {

                SettingRow("System Prompt", Icons.Outlined.Chat) {
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan, unfocusedBorderColor = BgBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = AccentCyan, focusedContainerColor = BgElevated,
                            unfocusedContainerColor = BgElevated,
                            focusedLabelColor = AccentCyan
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.updateSystemPrompt(systemPrompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgDeep),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Prompt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                SettingRow("Temperature: ${"%.2f".format(temperature)}", Icons.Outlined.Thermostat) {
                    Slider(
                        value = temperature, onValueChange = { temperature = it },
                        valueRange = 0f..1f, steps = 19,
                        onValueChangeFinished = { vm.updateTemperature(temperature) },
                        colors = SliderDefaults.colors(activeTrackColor = AccentCyan, thumbColor = AccentCyan,
                            inactiveTrackColor = BgElevated)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precise", color = TextMuted, fontSize = 10.sp)
                        Text("Creative", color = TextMuted, fontSize = 10.sp)
                    }
                }

                SettingRow("Max Tokens: $maxTokens", Icons.Outlined.Token) {
                    Slider(
                        value = maxTokens.toFloat(), onValueChange = { maxTokens = it.toInt() },
                        valueRange = 256f..32768f, steps = 62,
                        onValueChangeFinished = { vm.updateMaxTokens(maxTokens) },
                        colors = SliderDefaults.colors(activeTrackColor = AccentBlue, thumbColor = AccentBlue,
                            inactiveTrackColor = BgElevated)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("256", color = TextMuted, fontSize = 10.sp)
                        Text("32768 (Gemma 4)", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }

            // ── Vision section ────────────────────────────────────────────────
            val supportsVision by vm.supportsVision.collectAsState()
            SettingSection("Vision", Icons.Outlined.Visibility) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (supportsVision) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (supportsVision) AccentCyan else TextMuted
                        )
                        Text(
                            if (supportsVision) "Image input enabled" else "Image input not available",
                            color = if (supportsVision) TextPrimary else TextSecond,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        Modifier
                            .background(
                                if (supportsVision) Color(0xFF0D2A1A) else Color(0xFF1A1A1A),
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                0.5.dp,
                                if (supportsVision) AccentCyan.copy(0.4f) else BgBorder,
                                RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (supportsVision) "SUPPORTED" else "NOT SUPPORTED",
                            color = if (supportsVision) AccentCyan else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!supportsVision) {
                    Text(
                        "Load a vision-capable model (e.g. Gemma 3n E2B, Gemma 4 E2B) from the Models tab to enable image input.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // ── Appearance section ────────────────────────────────────────────
            SettingSection("Appearance", Icons.Outlined.Palette) {

                // Font Family picker — like Claude
                SettingRow("Font Style", Icons.Outlined.TextFields) {
                    Spacer(Modifier.height(4.dp))
                    FontFamilyPicker(
                        selected = settings.fontFamily,
                        onSelect = { vm.updateFontFamily(it) }
                    )
                }

                SettingRow("Font Size: ${fontSize}sp", Icons.Outlined.FormatSize) {
                    Slider(
                        value = fontSize.toFloat(), onValueChange = { fontSize = it.toInt() },
                        valueRange = 10f..22f, steps = 11,
                        onValueChangeFinished = { vm.updateFontSize(fontSize) },
                        colors = SliderDefaults.colors(activeTrackColor = AccentPurple, thumbColor = AccentPurple,
                            inactiveTrackColor = BgElevated)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10sp", color = TextMuted, fontSize = 10.sp)
                        Text("22sp", color = TextMuted, fontSize = 10.sp)
                    }
                    // Live preview
                    Box(
                        Modifier.fillMaxWidth()
                            .background(BgElevated, RoundedCornerShape(8.dp))
                            .border(0.5.dp, BgBorderBright, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "The quick brown fox jumps over the lazy dog.",
                            color = TextSecond,
                            fontSize = fontSize.sp,
                            fontFamily = when (settings.fontFamily) {
                                "serif" -> FontFamily.Serif
                                "mono"  -> FontFamily.Monospace
                                else    -> FontFamily.Default
                            },
                            lineHeight = (fontSize * 1.5).sp
                        )
                    }
                }
            }

            // ── About section ─────────────────────────────────────────────────
            SettingSection("About", Icons.Outlined.Info) {
                AboutRow("Version",   BuildConfig.VERSION_NAME,       Icons.Outlined.Tag)
                AboutRow("Build",     BuildConfig.VERSION_CODE.toString(), Icons.Outlined.Numbers)
                AboutRow("API Port",  ":${BuildConfig.API_PORT}",     Icons.Outlined.Api)
                AboutRow("Engine",    "LiteRT-LM",                    Icons.Outlined.Memory)
                AboutRow("Privacy",   "100% on-device · no telemetry",Icons.Outlined.Lock)
            }
        }
    }
}

// ── Font family picker — 3 buttons like Claude ────────────────────────────────
@Composable
private fun FontFamilyPicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("sans",  "Sans-serif", FontFamily.Default),
        Triple("serif", "Serif",      FontFamily.Serif),
        Triple("mono",  "Monospace",  FontFamily.Monospace),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label, ff) ->
            val isSelected = selected == key
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (isSelected)
                            Brush.horizontalGradient(listOf(AccentCyan.copy(0.12f), AccentPurple.copy(0.08f)))
                        else
                            Brush.horizontalGradient(listOf(BgElevated, BgElevated)),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        if (isSelected) 1.dp else 0.5.dp,
                        if (isSelected) AccentCyan.copy(0.5f) else BgBorderBright,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Aa",
                        fontFamily = ff,
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AccentCyan else TextSecond
                    )
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontFamily = ff,
                        color = if (isSelected) AccentCyan else TextMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── About row ─────────────────────────────────────────────────────────────────
@Composable
private fun AboutRow(label: String, value: String, icon: ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = TextMuted)
        Text(label, color = TextSecond, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

// ── Section container ─────────────────────────────────────────────────────────
@Composable
fun SettingSection(title: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(13.dp), tint = TextMuted)
            Text(title.uppercase(), color = TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSurface),
            border = BorderStroke(0.5.dp, BgBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
            }
        }
    }
}

// ── Setting row ───────────────────────────────────────────────────────────────
@Composable
fun SettingRow(label: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(13.dp), tint = TextSecond)
            Text(label, color = TextSecond, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        content()
    }
}
