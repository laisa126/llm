package com.laiserdev.localllm.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.BuildConfig
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenDrawer: () -> Unit = {}) {
    val settings by vm.settings.collectAsState()
    var systemPrompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var temperature by remember(settings.temperature) { mutableFloatStateOf(settings.temperature) }
    var maxTokens by remember(settings.maxTokens) { mutableIntStateOf(settings.maxTokens) }
    var fontSize by remember(settings.fontSize) { mutableIntStateOf(settings.fontSize) }

    Column(Modifier.fillMaxSize().background(BgDeep).verticalScroll(rememberScrollState())) {
        com.laiserdev.localllm.ui.AppTopBar("Settings", onOpenDrawer)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            SettingSection("Model") {
                SettingRow("System Prompt") {
                    OutlinedTextField(
                        value = systemPrompt, onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = BgBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = AccentGreen, focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        maxLines = 5
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { vm.updateSystemPrompt(systemPrompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Save", fontSize = 12.sp)
                    }
                }

                SettingRow("Temperature: ${"%.2f".format(temperature)}") {
                    Slider(value = temperature, onValueChange = { temperature = it },
                        valueRange = 0f..1f, steps = 19,
                        onValueChangeFinished = { vm.updateTemperature(temperature) },
                        colors = SliderDefaults.colors(activeTrackColor = AccentGreen, thumbColor = AccentGreen)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precise", color = TextMuted, fontSize = 10.sp)
                        Text("Creative", color = TextMuted, fontSize = 10.sp)
                    }
                }

                SettingRow("Max Tokens: $maxTokens") {
                    Slider(value = maxTokens.toFloat(), onValueChange = { maxTokens = it.toInt() },
                        valueRange = 256f..8192f, steps = 30,
                        onValueChangeFinished = { vm.updateMaxTokens(maxTokens) },
                        colors = SliderDefaults.colors(activeTrackColor = AccentBlue, thumbColor = AccentBlue)
                    )
                }
            }

            SettingSection("Editor") {
                SettingRow("Font Size: ${fontSize}sp") {
                    Slider(value = fontSize.toFloat(), onValueChange = { fontSize = it.toInt() },
                        valueRange = 10f..20f, steps = 9,
                        onValueChangeFinished = { vm.updateFontSize(fontSize) },
                        colors = SliderDefaults.colors(activeTrackColor = AccentPurple, thumbColor = AccentPurple)
                    )
                }
            }

            SettingSection("HuggingFace") {
                val hfToken = settings.hfToken
                var tokenInput by remember(hfToken) { mutableStateOf(hfToken) }
                var tokenVisible by remember { mutableStateOf(false) }

                SettingRow("Access Token") {
                    Text(
                        "Required to download Gemma models. Accept the license at huggingface.co/google/gemma then get a token at huggingface.co/settings/tokens",
                        color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxx", color = TextMuted, fontSize = 12.sp) },
                        visualTransformation = if (tokenVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, Modifier.size(18.dp), tint = TextMuted
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = BgBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentGreen,
                            focusedContainerColor = BgElevated,
                            unfocusedContainerColor = BgElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { vm.updateHfToken(tokenInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Token")
                    }
                    if (hfToken.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(13.dp), tint = AccentGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Token saved — downloads should work now", color = AccentGreen, fontSize = 11.sp)
                        }
                    }
                }
            }

            SettingSection("About") {
                SettingRow("Version") {
                    Text(BuildConfig.VERSION_NAME, color = TextSecond, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace)
                }
                SettingRow("Build") {
                    Text(BuildConfig.VERSION_CODE.toString(), color = TextSecond, fontSize = 13.sp)
                }
                SettingRow("API Port") {
                    Text(":${BuildConfig.API_PORT}", color = AccentGreen, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = BgSurface),
            border = BorderStroke(0.5.dp, BgBorder), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingRow(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(label, color = TextSecond, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}
