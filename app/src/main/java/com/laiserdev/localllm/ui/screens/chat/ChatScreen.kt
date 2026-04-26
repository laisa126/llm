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
import com.laiserdev.localllm.ui.screens.chat.StepStatus
import com.laiserdev.localllm.ui.theme.*

@Composable
fun ChatScreen(vm: MainViewModel) {
    val messages by vm.messages.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val isAgentRunning by vm.isAgentRunning.collectAsState()
    val selectedImage by vm.selectedImageUri.collectAsState()
    val skills by vm.skills.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val agentSteps by vm.agentSteps.collectAsState()
    val agentThinking by vm.agentThinking.collectAsState()
    val chatSessions by vm.chatSessions.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var agentMode by remember { mutableStateOf(false) }
    var showSkills by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var selectedSkill by remember { mutableStateOf<Skill?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> vm.setSelectedImage(uri) }

    // Auto-scroll when new steps or messages arrive
    val totalItems = messages.size + agentSteps.size
    LaunchedEffect(totalItems) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(
            (messages.size + agentSteps.size + 1).coerceAtLeast(0)
        )
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
                    Text(activeProject!!.name, style = MaterialTheme.typography.bodySmall, color = TextSecond)
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
                IconButton(onClick = { vm.refreshChatSessions(); showHistory = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.History, "History", tint = TextSecond, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { vm.clearChat() }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Clear", tint = TextSecond, modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)

        // ── Agent mode banner ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = agentMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF0D1F17))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartToy, null, Modifier.size(13.dp), tint = AccentGreen)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (activeProject != null) "Agent will build inside: ${activeProject!!.name}"
                    else "⚠ Open a project in Editor first to use Agent mode",
                    color = if (activeProject != null) AccentGreen else WarnYellow,
                    fontSize = 11.sp
                )
            }
        }

        // ── Messages ───────────────────────────────────────────────────────────
        if (messages.isEmpty() && agentSteps.isEmpty()) {
            EmptyState(onSkillsClick = { showSkills = true }, onSuggestion = { inputText = it })
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

                // Agent steps shown below last message while agent is running
                if (isAgentRunning && (agentSteps.isNotEmpty() || agentThinking != null)) {
                    item(key = "agent_steps") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(BgSurface, RoundedCornerShape(10.dp))
                                .border(0.5.dp, BgBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SmartToy, null, Modifier.size(13.dp), tint = AccentGreen)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Agent Working", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("${agentSteps.size} steps", color = TextMuted, fontSize = 10.sp)
                            }
                            AgentStepsPanel(
                                steps = agentSteps,
                                thinking = agentThinking
                            )
                        }
                    }
                }

                // After agent finishes, show a compact summary
                if (!isAgentRunning && agentSteps.isNotEmpty()) {
                    item(key = "agent_summary") {
                        AgentSummaryRow(agentSteps)
                    }
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
                    focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated
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

    // ── Chat History sheet ─────────────────────────────────────────────────────
    if (showHistory) {
        ChatHistorySheet(
            sessions = chatSessions,
            onLoad = { fileName -> vm.loadChatSession(fileName) },
            onDelete = { fileName -> vm.deleteChatSession(fileName) },
            onDismiss = { showHistory = false }
        )
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
            extractVars = { vm.skills.value },
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
fun AgentSummaryRow(steps: List<AgentStep>) {
    val successCount = steps.count { it.status == StepStatus.SUCCESS }
    val errorCount = steps.count { it.status == StepStatus.ERROR }
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(8.dp))
            .border(0.5.dp, BgBorder, RoundedCornerShape(8.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(13.dp), tint = AccentGreen)
            Spacer(Modifier.width(6.dp))
            Text(
                "Agent completed · $successCount tools used" + if (errorCount > 0) " · $errorCount errors" else "",
                color = if (errorCount > 0) WarnYellow else AccentGreen,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, Modifier.size(14.dp), tint = TextMuted
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier.padding(horizontal = 10.dp).padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                steps.forEach { step -> AgentStepCard(step) }
            }
        }
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
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (isUser) {
                // User bubble — simple
                Box(
                    Modifier
                        .background(Color(0xFF1A3A2A), RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp))
                        .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (msg.imageUri != null) {
                        AsyncImage(
                            model = msg.imageUri, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (msg.content.isNotBlank()) Spacer(Modifier.height(6.dp))
                    }
                    if (msg.content.isNotBlank()) {
                        Text(msg.content, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            } else {
                // Assistant bubble — parse code blocks
                AssistantContent(msg)
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
fun AssistantContent(msg: ChatMessage) {
    val segments = remember(msg.content) { parseMessageSegments(msg.content) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        segments.forEach { seg ->
            when (seg) {
                is MessageSegment.Text -> {
                    if (seg.text.isNotBlank()) {
                        Box(
                            Modifier
                                .background(BgSurface, RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp))
                                .border(0.5.dp, BgBorder, RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(seg.text.trim(), color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
                is MessageSegment.Code -> {
                    CodeBlock(language = seg.language, code = seg.code)
                }
            }
        }
        if (msg.isStreaming) {
            Text("▋", color = AccentGreen, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
            .border(0.5.dp, BgBorder, RoundedCornerShape(8.dp))
    ) {
        // Header bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(BgElevated, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                language.ifBlank { "code" }.lowercase(),
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                    copied = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    null,
                    Modifier.size(14.dp),
                    tint = if (copied) AccentGreen else TextMuted
                )
            }
        }
        // Code content
        Text(
            text = code.trimEnd(),
            color = Color(0xFFE6EDF3),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }

    // Reset "copied" after 2s
    if (copied) {
        LaunchedEffect(copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
}

sealed class MessageSegment {
    data class Text(val text: String) : MessageSegment()
    data class Code(val language: String, val code: String) : MessageSegment()
}

fun parseMessageSegments(content: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val regex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    var lastEnd = 0
    for (match in regex.findAll(content)) {
        if (match.range.first > lastEnd) {
            segments += MessageSegment.Text(content.substring(lastEnd, match.range.first))
        }
        segments += MessageSegment.Code(
            language = match.groupValues[1],
            code = match.groupValues[2]
        )
        lastEnd = match.range.last + 1
    }
    if (lastEnd < content.length) {
        segments += MessageSegment.Text(content.substring(lastEnd))
    }
    return segments.ifEmpty { listOf(MessageSegment.Text(content)) }
}

@Composable
fun EmptyState(onSkillsClick: () -> Unit, onSuggestion: (String) -> Unit = {}) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clean logo — no emoji
        Box(
            Modifier
                .size(56.dp)
                .background(Color(0xFF1A3A2A), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Memory, null, Modifier.size(28.dp), tint = AccentGreen)
        }
        Spacer(Modifier.height(14.dp))
        Text("LocalLLM", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text("Local AI coding assistant", color = TextSecond, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        // Suggestion chips — 2-column grid, actually clickable
        val suggestions = listOf(
            "Generate a React todo app",
            "Fix a bug in my code",
            "Explain this function",
            "Write a Python script"
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            suggestions.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { s ->
                        Box(
                            Modifier
                                .weight(1f)
                                .background(BgElevated, RoundedCornerShape(10.dp))
                                .border(0.5.dp, BgBorder, RoundedCornerShape(10.dp))
                                .clickable { onSuggestion(s) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(s, color = TextSecond, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                    // Pad last row if odd
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSkillsClick,
            border = BorderStroke(1.dp, AccentGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Browse Skills", fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsSheet(skills: List<Skill>, onSelect: (Skill) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
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
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
        containerColor = BgSurface,
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
                            cursorColor = AccentGreen, focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated,
                            focusedLabelColor = AccentGreen
                        ),
                        modifier = Modifier.fillMaxWidth(), maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(values.toMap()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep)) {
                Text("Apply Skill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecond) }
        }
    )
}


