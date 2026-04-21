package com.laiserdev.localllm.ui.screens.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.data.model.TerminalLine
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

@Composable
fun TerminalScreen(vm: MainViewModel) {
    val lines by vm.terminalLines.collectAsState()
    val isRunning by vm.isRunningCommand.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }
    var historyIdx by remember { mutableIntStateOf(-1) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var showPackagePanel by remember { mutableStateOf(false) }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Column(Modifier.fillMaxSize().background(BgDeep)) {

        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(BgSurface).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Color(0xFFFF5F57), RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(10.dp).background(Color(0xFFFFBD2E), RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(10.dp).background(AccentGreen, RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(12.dp))
                Text(
                    activeProject?.let { "📁 ${it.name}" } ?: "Terminal",
                    color = TextSecond, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showPackagePanel = !showPackagePanel }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Extension, "Packages", tint = AccentBlue, modifier = Modifier.size(16.dp))
                }
                if (isRunning) {
                    IconButton(onClick = { vm.terminalLines }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.Stop, "Kill", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = { vm.clearTerminal() }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.CleaningServices, "Clear", tint = TextSecond, modifier = Modifier.size(16.dp))
                }
                if (activeProject != null) {
                    IconButton(onClick = { vm.runProjectAutoDetect() }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, "Run", tint = AccentGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        // ── Package installer panel ────────────────────────────────────────────
        AnimatedVisibility(showPackagePanel) {
            PackageInstallerPanel(onInstall = { mgr, pkgs ->
                vm.installPackages(mgr, pkgs)
                showPackagePanel = false
            })
        }

        // ── Terminal output ────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(lines, key = { "${it.timestamp}-${it.text.take(10)}" }) { line ->
                TerminalLineItem(line)
            }
            if (isRunning) {
                item {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(10.dp), color = AccentGreen, strokeWidth = 1.5.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Running...", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        // ── Input ──────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(BgSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("❯", color = AccentGreen, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it; historyIdx = -1 },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(AccentGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && !isRunning) {
                        val cmd = input.trim()
                        history.add(0, cmd)
                        historyIdx = -1
                        input = ""
                        keyboard?.hide()
                        vm.runCommand(cmd)
                    }
                }),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text("Enter command...", color = TextMuted, fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                    inner()
                }
            )

            // Quick command suggestions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 4.dp)) {
                listOf("ls", "pwd", "clear").forEach { cmd ->
                    Box(
                        Modifier.background(BgElevated, RoundedCornerShape(4.dp))
                            .clickable { vm.runCommand(cmd) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(cmd, color = TextSecond, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLineItem(line: TerminalLine) {
    val (color, prefix) = when (line.type) {
        TerminalLine.LineType.COMMAND -> AccentGreen to "❯ "
        TerminalLine.LineType.ERROR   -> ErrorRed to ""
        TerminalLine.LineType.INFO    -> AccentBlue to "ℹ "
        TerminalLine.LineType.TOOL    -> AccentPurple to ""
        else                          -> TextPrimary to ""
    }
    Text(
        text = "$prefix${line.text}",
        color = color,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp)
    )
}

@Composable
fun PackageInstallerPanel(onInstall: (String, List<String>) -> Unit) {
    var manager by remember { mutableStateOf("npm") }
    var packages by remember { mutableStateOf("") }
    val managers = listOf("npm", "pip", "apt", "yarn")

    Column(
        Modifier.fillMaxWidth().background(BgSurface).border(BorderStroke(0.5.dp, BgBorder))
            .padding(12.dp)
    ) {
        Text("📦 Install Packages", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            managers.forEach { m ->
                FilterChip(
                    selected = manager == m, onClick = { manager = m },
                    label = { Text(m, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A2A3A),
                        selectedLabelColor = AccentBlue
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = packages, onValueChange = { packages = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("express react axios...", color = TextMuted, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue, unfocusedBorderColor = BgBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue, containerColor = BgElevated
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
            Button(
                onClick = {
                    val pkgs = packages.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (pkgs.isNotEmpty()) onInstall(manager, pkgs)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) { Text("Install", fontSize = 12.sp) }
        }
    }
}
