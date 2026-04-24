package com.laiserdev.localllm.ui.screens.editor

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.data.model.ProjectFile
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*
import java.io.File
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp

@Composable
fun EditorScreen(vm: MainViewModel) {
    val activeProject by vm.activeProject.collectAsState()
    val fileTree by vm.fileTree.collectAsState()
    val openFiles by vm.openFiles.collectAsState()
    val activeFilePath by vm.activeFilePath.collectAsState()
    val projects by vm.projects.collectAsState()
    val isAgentRunning by vm.isAgentRunning.collectAsState()

    var showFileTree by remember { mutableStateOf(true) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showAIPanel by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize().background(BgDeep)) {

        // ── File tree sidebar ──────────────────────────────────────────────────
        AnimatedVisibility(showFileTree, enter = slideInHorizontally(), exit = slideOutHorizontally()) {
            Column(
                Modifier.width(220.dp).fillMaxHeight().background(BgSurface)
                    .border(end = 0.5.dp, color = BgBorder)
            ) {
                // Sidebar header
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EXPLORER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp))
                    Row {
                        IconButton(onClick = { showNewFileDialog = true }, Modifier.size(24.dp)) {
                            Icon(Icons.Default.Add, null, tint = TextSecond, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = { showNewProjectDialog = true }, Modifier.size(24.dp)) {
                            Icon(Icons.Default.CreateNewFolder, null, tint = TextSecond, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Project selector
                if (projects.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(0.5.dp, BgBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecond)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, Modifier.size(14.dp), tint = AccentOrange)
                            Spacer(Modifier.width(4.dp))
                            Text(activeProject?.name ?: "Open Project",
                                fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                            modifier = Modifier.background(BgElevated)) {
                            projects.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name, color = TextPrimary, fontSize = 12.sp) },
                                    onClick = { expanded = false; vm.openProject(p) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BgBorder, thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))

                // File tree
                if (fileTree != null) {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                        fileTree!!.children.forEach { child ->
                            fileTreeItem(child, 0, activeFilePath, vm)
                        }
                    }
                } else {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No project open", color = TextMuted, fontSize = 12.sp)
                    }
                }

                // Sidebar actions
                HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { vm.exportProject() }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.FileDownload, "Export ZIP", tint = TextSecond, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { vm.runProjectAutoDetect() }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, "Run", tint = AccentGreen, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showAIPanel = !showAIPanel }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.AutoAwesome, "AI", tint = AccentBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Main editor area ───────────────────────────────────────────────────
        Column(Modifier.weight(1f).fillMaxHeight()) {

            // Tabs
            if (openFiles.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().background(BgSurface).horizontalScroll(rememberScrollState()),
                ) {
                    IconButton(onClick = { showFileTree = !showFileTree }, Modifier.size(36.dp)) {
                        Icon(if (showFileTree) Icons.Default.MenuOpen else Icons.Default.Menu,
                            null, tint = TextSecond, modifier = Modifier.size(16.dp))
                    }
                    openFiles.forEach { (path, _) ->
                        val isActive = path == activeFilePath
                        val name = File(path).name
                        Row(
                            Modifier
                                .background(if (isActive) BgDeep else BgSurface)
                                .border(bottom = if (isActive) 2.dp else 0.dp, color = if (isActive) AccentGreen else Color.Transparent)
                                .clickable { vm.openFile(path) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = if (isActive) TextPrimary else TextSecond, fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Close, null,
                                Modifier.size(12.dp).clickable { vm.closeFile(path) },
                                tint = TextMuted)
                        }
                    }
                }
                HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
            }

            // Code editor
            val activeContent = openFiles.firstOrNull { it.first == activeFilePath }
            if (activeContent != null) {
                var codeText by remember(activeFilePath) { mutableStateOf(activeContent.second) }
                var isDirty by remember(activeFilePath) { mutableStateOf(false) }

                // Toolbar
                Row(
                    Modifier.fillMaxWidth().background(BgSurface).padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(File(activeFilePath!!).name, color = TextSecond, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isDirty) {
                            Button(
                                onClick = { vm.saveFile(activeFilePath!!, codeText); isDirty = false },
                                Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep)
                            ) { Text("Save", fontSize = 11.sp) }
                        }
                        IconButton(onClick = { showAIPanel = true; aiPrompt = "Explain this file:\n\n$codeText" },
                            Modifier.size(26.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Text editor (Sora editor would be here in full impl - using BasicTextField for now)
                TextField(
                    value = codeText,
                    onValueChange = { codeText = it; isDirty = true },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BgDeep,
                        unfocusedContainerColor = BgDeep,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    )
                )
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Code, null, Modifier.size(48.dp), tint = TextMuted)
                        Spacer(Modifier.height(8.dp))
                        Text("Select a file to edit", color = TextMuted, fontSize = 14.sp)
                        if (activeProject == null) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { showNewProjectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep)) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Project")
                            }
                        }
                    }
                }
            }
        }

        // ── AI Assistant panel ─────────────────────────────────────────────────
        AnimatedVisibility(showAIPanel, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            Column(
                Modifier.width(280.dp).fillMaxHeight().background(BgSurface)
                    .border(start = 0.5.dp, color = BgBorder)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Assistant", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { showAIPanel = false }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextSecond, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
                OutlinedTextField(
                    value = aiPrompt, onValueChange = { aiPrompt = it },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                    placeholder = { Text("Ask AI about this code...", color = TextMuted, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = BgBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue, focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    maxLines = 20
                )
                Button(
                    onClick = {
                        if (aiPrompt.isNotBlank()) {
                            vm.sendMessage(aiPrompt)
                            showAIPanel = false
                            aiPrompt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    enabled = aiPrompt.isNotBlank() && !isAgentRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ask AI")
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onConfirm = { name -> showNewProjectDialog = false; vm.createProject(name) },
            onDismiss = { showNewProjectDialog = false }
        )
    }

    if (showNewFileDialog && activeProject != null) {
        NewFileDialog(
            onConfirm = { path -> showNewFileDialog = false; vm.createNewFile(path) },
            onDismiss = { showNewFileDialog = false }
        )
    }
}

fun LazyListScope.fileTreeItem(
    file: ProjectFile, depth: Int,
    activeFilePath: String?, vm: MainViewModel
) {
    item(key = file.absolutePath) {
        var expanded by remember { mutableStateOf(depth < 1) }
        val isActive = file.absolutePath == activeFilePath
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (isActive) Color(0xFF1A3A2A) else Color.Transparent)
                .clickable {
                    if (file.isDirectory) expanded = !expanded
                    else vm.openFile(file.absolutePath)
                }
                .padding(start = (16 + depth * 12).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file.isDirectory) {
                    if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else Icons.Default.InsertDriveFile,
                null,
                Modifier.size(14.dp),
                tint = if (file.isDirectory) AccentOrange
                else extensionColor(file.extension)
            )
            Spacer(Modifier.width(6.dp))
            Text(file.name, color = if (isActive) AccentGreen else TextPrimary, fontSize = 12.sp,
                modifier = Modifier.weight(1f))
        }
    }
    if (file.isDirectory && file.children.isNotEmpty()) {
        // Expanded state is tracked per item — simplified here
        file.children.forEach { child ->
            fileTreeItem(child, depth + 1, activeFilePath, vm)
        }
    }
}

@Composable
fun NewProjectDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgSurface,
        title = { Text("New Project", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Project name", color = TextSecond) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen, unfocusedBorderColor = BgBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGreen, focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated,
                    focusedLabelColor = AccentGreen
                ),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep)) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecond) } }
    )
}

@Composable
fun NewFileDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var path by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgSurface,
        title = { Text("New File", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Relative path (e.g. src/index.js)", color = TextSecond, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = path, onValueChange = { path = it },
                    placeholder = { Text("src/index.js", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen, unfocusedBorderColor = BgBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = AccentGreen, focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated
                    ),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (path.isNotBlank()) onConfirm(path.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep)) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecond) } }
    )
}

private fun Modifier.border(start: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp, color: Color) =
    this.then(
        if (start > 0.dp || end > 0.dp || bottom > 0.dp)
            Modifier.drawBehind {
                if (start > 0.dp) drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), start.toPx())
                if (end > 0.dp) drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), end.toPx())
                if (bottom > 0.dp) drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), bottom.toPx())
            }
        else Modifier
    )

