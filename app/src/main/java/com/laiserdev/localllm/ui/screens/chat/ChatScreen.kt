package com.laiserdev.localllm.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.laiserdev.localllm.data.model.ChatMessage
import com.laiserdev.localllm.data.model.MessageRole
import com.laiserdev.localllm.data.model.Skill
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*

@Composable
fun ChatScreen(vm: MainViewModel) {
    val messages by vm.messages.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val isAgentRunning by vm.isAgentRunning.collectAsState()
    val selectedImage by vm.selectedImageUri.collectAsState()
    val skills by vm.skills.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var agentMode by remember { mutableStateOf(false) }
    var showSkills by remember { mutableStateOf(false) }
    var selectedSkill by remember { mutableStateOf<Skill?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> vm.setSelectedImage(uri) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize().background(BgDeep)) {

        // ── Top bar ────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(BgSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LocalLLM", style = MaterialTheme.typography.titleMedium, color = AccentGreen,
                    fontWeight = FontWeight.Bold)
                if (activeProject != null) {
                    Text("📁 ${activeProject!!.name}", style = MaterialTheme.typography.bodySmall, color = TextSecond)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Agent mode toggle
                FilterChip(
                    selected = agentMode,
                    onClick = { agentMode = !agentMode },
                    label = { Text("Agent", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.SmartToy, null, Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A3A2A),
                        selectedLabelColor = AccentGreen,
                        selectedLeadingIconColor = AccentGreen
                    )
                )
                IconButton(onClick = { showSkills = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.AutoAwesome, "Skills", tint = AccentBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { vm.clearChat() }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Clear", tint = TextSecond, modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        // ── Messages ───────────────────────────────────────────────────────────
        if (messages.isEmpty()) {
            EmptyState(onSkillsClick = { showSkills = true })
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
            }
        }

        // ── Selected image preview ─────────────────────────────────────────────
        AnimatedVisibility(selectedImage != null) {
            Row(Modifier.fillMaxWidth().background(BgSurface).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = selectedImage, contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text("Image attached", color = TextSecond, fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.setSelectedImage(null) }, Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextSecond, modifier = Modifier.size(16.dp))
                }
            }
        }

        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        // ── Input bar ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(BgSurface).padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { imagePickerLauncher.launch("image/*") }, Modifier.size(40.dp)) {
                Icon(Icons.Default.Image, "Image", tint = TextSecond, modifier = Modifier.size(20.dp))
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        if (agentMode) "Ask agent to build something..." else "Message LocalLLM...",
                        color = TextMuted, fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BgBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGreen,
                    unfocusedContainerColor = BgElevated,
                    focusedContainerColor = BgElevated
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
            val busy = isGenerating || isAgentRunning
            IconButton(
                onClick = {
                    if (inputText.isBlank()) return@IconButton
                    val text = inputText.trim()
                    inputText = ""
                    if (agentMode && activeProject != null) vm.runAgent(text)
                    else vm.sendMessage(text, vm.selectedImageUri.value)
                },
                modifier = Modifier.size(40.dp)
                    .background(if (busy) BgElevated else AccentGreen, CircleShape),
                enabled = inputText.isNotBlank() && !busy
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = AccentGreen, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, "Send", tint = BgDeep, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // ── Skills bottom sheet ────────────────────────────────────────────────────
    if (showSkills) {
        SkillsSheet(
            skills = skills,
            onSelect = { skill ->
                showSkills = false
                selectedSkill = skill
            },
            onDismiss = { showSkills = false }
        )
    }

    selectedSkill?.let { skill ->
        SkillVariablesDialog(
            skill = skill,
            extractVars = { vm.skills.value }, // pass through
            skillsManager = null,
            onConfirm = { vars ->
                selectedSkill = null
                vm.applySkill(skill, vars)
            },
            onDismiss = { selectedSkill = null }
        )
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == MessageRole.USER
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                Modifier.size(28.dp).background(AccentGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("AI", fontSize = 10.sp, color = BgDeep, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .background(
                        if (isUser) Color(0xFF1A3A2A) else BgSurface,
                        RoundedCornerShape(
                            topStart = if (isUser) 12.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 12.dp,
                            bottomStart = 12.dp, bottomEnd = 12.dp
                        )
                    )
                    .border(0.5.dp, BgBorder, RoundedCornerShape(
                        topStart = if (isUser) 12.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 12.dp,
                        bottomStart = 12.dp, bottomEnd = 12.dp
                    ))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (msg.imageUri != null) {
                    AsyncImage(
                        model = msg.imageUri, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (msg.content.isNotBlank()) {
                    Text(
                        text = msg.content,
                        color = if (isUser) TextPrimary else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = if (msg.content.contains("```")) MonoFont else FontFamily.Default
                    )
                }
                if (msg.isStreaming) {
                    Text("▋", color = AccentGreen, fontSize = 14.sp)
                }
            }
            if (msg.tokensPerSecond > 0) {
                Text(
                    "${"%.1f".format(msg.tokensPerSecond)} tok/s",
                    color = TextMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(onSkillsClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("LocalLLM", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Local AI coding assistant", color = TextSecond, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        val suggestions = listOf(
            "Generate a React todo app", "Fix a bug in my code",
            "Explain this function", "Write a Python script"
        )
        suggestions.forEach { s ->
            SuggestionChip(onClick = {}, label = { Text(s, fontSize = 12.sp) },
                modifier = Modifier.padding(vertical = 3.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    unfocusedContainerColor = BgElevated,
                    focusedContainerColor = BgElevated, labelColor = TextSecond
                ),
                border = BorderStroke(0.5.dp, BgBorder)
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onSkillsClick,
            border = BorderStroke(1.dp, AccentGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen)
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Browse Skills")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsSheet(skills: List<Skill>, onSelect: (Skill) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        unfocusedContainerColor = BgSurface,
                    focusedContainerColor = BgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BgBorder) }
    ) {
        Text("Skills", style = MaterialTheme.typography.titleMedium,
            color = TextPrimary, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        val grouped = skills.groupBy { it.category }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            grouped.forEach { (cat, catSkills) ->
                item {
                    Text(cat.uppercase(), color = TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                items(catSkills) { skill ->
                    ListItem(
                        headlineContent = {
                            Text("${skill.icon} ${skill.name}", color = TextPrimary, fontSize = 14.sp) },
                        supportingContent = {
                            Text(skill.description, color = TextSecond, fontSize = 12.sp) },
                        modifier = Modifier.clickable { onSelect(skill) },
                        colors = ListItemDefaults.colors(unfocusedContainerColor = Color,
                    focusedContainerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun SkillVariablesDialog(
    skill: Skill, extractVars: (List<Skill>) -> List<Skill>,
    skillsManager: Any?,
    onConfirm: (Map<String, String>) -> Unit, onDismiss: () -> Unit
) {
    val vars = remember {
        Regex("\\{\\{([A-Z_]+)\\}\\}")
            .findAll(skill.userPromptTemplate)
            .map { it.groupValues[1] }.distinct().toList()
    }
    val values = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        unfocusedContainerColor = BgSurface,
                    focusedContainerColor = BgSurface,
        title = { Text("${skill.icon} ${skill.name}", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vars.forEach { v ->
                    OutlinedTextField(
                        value = values[v] ?: "",
                        onValueChange = { values[v] = it },
                        label = { Text(v.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            color = TextSecond, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = BgBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = AccentGreen, unfocusedContainerColor = BgElevated,
                    focusedContainerColor = BgElevated,
                            focusedLabelColor = AccentGreen
                        ),
                        modifier = Modifier.fillMaxWidth(), maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(values.toMap()) },
                colors = ButtonDefaults.buttonColors(unfocusedContainerColor = AccentGreen,
                    focusedContainerColor = AccentGreen, contentColor = BgDeep)) {
                Text("Apply Skill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecond) }
        }
    )
}

private val MonoFont = FontFamily.Monospace
