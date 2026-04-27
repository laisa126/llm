package com.laiserdev.localllm.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import coil.compose.AsyncImage
import com.laiserdev.localllm.data.model.ChatMessage
import com.laiserdev.localllm.data.model.MessageRole
import com.laiserdev.localllm.data.model.Skill
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.theme.*

@Composable
fun ChatScreen(vm: MainViewModel, onOpenDrawer: () -> Unit = {}) {
    val messages       by vm.messages.collectAsState()
    val isGenerating   by vm.isGenerating.collectAsState()
    val isAgentRunning by vm.isAgentRunning.collectAsState()
    val selectedImage  by vm.selectedImageUri.collectAsState()
    val skills         by vm.skills.collectAsState()
    val activeProject  by vm.activeProject.collectAsState()
    val agentSteps     by vm.agentSteps.collectAsState()
    val agentThinking  by vm.agentThinking.collectAsState()
    val chatSessions   by vm.chatSessions.collectAsState()
    val settings       by vm.settings.collectAsState()
    val models         by vm.models.collectAsState()

    val listState = rememberLazyListState()
    var inputText     by remember { mutableStateOf("") }
    var agentMode     by remember { mutableStateOf(false) }
    var showSkills    by remember { mutableStateOf(false) }
    var showHistory   by remember { mutableStateOf(false) }
    var selectedSkill by remember { mutableStateOf<Skill?>(null) }

    val loadedModelName = remember(settings, models) {
        models.firstOrNull { it.id == settings.activeModelId && it.status.name == "LOADED" }?.name
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> vm.setSelectedImage(uri) }

    // Auto-scroll
    val totalItems = messages.size + agentSteps.size
    LaunchedEffect(totalItems) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size + agentSteps.size + 1).coerceAtLeast(0))
        }
    }

    Column(Modifier.fillMaxSize().background(BgDeep)) {

        // ── Claude-style top bar ───────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0F0A))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu
            IconButton(onClick = onOpenDrawer, Modifier.size(40.dp)) {
                Icon(Icons.Default.Menu, "Open menu", Modifier.size(22.dp), tint = TextSecond)
            }

            Spacer(Modifier.width(2.dp))

            // Model selector pill (center)
            Row(
                Modifier
                    .weight(1f)
                    .background(Color(0xFF141814), RoundedCornerShape(20.dp))
                    .border(0.5.dp, BgBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(
                            if (loadedModelName != null) AccentGreen else TextMuted,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    loadedModelName ?: "No model loaded",
                    color = if (loadedModelName != null) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp), tint = TextMuted)
            }

            Spacer(Modifier.width(4.dp))

            // Agent toggle
            Box(
                Modifier
                    .background(
                        if (agentMode) Color(0xFF1A3A2A) else Color(0xFF141814),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        0.5.dp,
                        if (agentMode) AccentGreen.copy(0.4f) else BgBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { agentMode = !agentMode }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SmartToy, null,
                        Modifier.size(14.dp),
                        tint = if (agentMode) AccentGreen else TextMuted
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Agent",
                        fontSize = 12.sp,
                        color = if (agentMode) AccentGreen else TextMuted,
                        fontWeight = if (agentMode) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // New chat button
            IconButton(onClick = { vm.clearChat() }, Modifier.size(40.dp)) {
                Icon(Icons.Default.EditNote, "New chat", Modifier.size(22.dp), tint = TextSecond)
            }
        }

        // ── Agent banner ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = agentMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Color(0xFF0D1F0D))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartToy, null, Modifier.size(12.dp), tint = AccentGreen)
                Spacer(Modifier.width(7.dp))
                Text(
                    if (activeProject != null) "Agent mode · ${activeProject!!.name}"
                    else "⚠ Open a project in Editor before using agent",
                    color = if (activeProject != null) AccentGreen else WarnYellow,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = BgBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

        // ── Messages ───────────────────────────────────────────────────────────
        if (messages.isEmpty() && agentSteps.isEmpty()) {
            EmptyState(
                modelName = loadedModelName,
                onSkillsClick = { showSkills = true },
                onSuggestion = { inputText = it }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }

                // Live agent steps
                if (isAgentRunning && (agentSteps.isNotEmpty() || agentThinking != null)) {
                    item(key = "agent_steps") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .background(BgSurface, RoundedCornerShape(12.dp))
                                .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SmartToy, null, Modifier.size(12.dp), tint = AccentGreen)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Agent working", color = AccentGreen, fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium)
                                }
                                Text("${agentSteps.size} steps", color = TextMuted, fontSize = 10.sp)
                            }
                            AgentStepsPanel(steps = agentSteps, thinking = agentThinking)
                        }
                    }
                }

                if (!isAgentRunning && agentSteps.isNotEmpty()) {
                    item(key = "agent_summary") {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            AgentSummaryRow(agentSteps)
                        }
                    }
                }
            }
        }

        // ── Image attachment preview ───────────────────────────────────────────
        AnimatedVisibility(selectedImage != null) {
            Row(
                Modifier.fillMaxWidth().background(BgSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = selectedImage, contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text("Image attached", color = TextSecond, fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.setSelectedImage(null) }, Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextSecond, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ── Input area ─────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0F0A))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Text field — Claude-style: full-width, rounded, dark
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141814), RoundedCornerShape(16.dp))
                    .border(0.5.dp, BgBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp), tint = TextMuted)
                }

                // Input field
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(AccentGreen),
                    maxLines = 6,
                    decorationBox = { inner ->
                        if (inputText.isEmpty()) {
                            Text(
                                if (agentMode) "Ask agent to build something…"
                                else "Message LocalLLM…",
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                )

                // Skills / history actions
                IconButton(
                    onClick = { showSkills = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(17.dp), tint = TextMuted)
                }

                // Send / stop button — Claude style
                val busy = isGenerating || isAgentRunning
                Box(
                    Modifier
                        .padding(end = 4.dp, bottom = 4.dp)
                        .size(34.dp)
                        .background(
                            if (inputText.isNotBlank() && !busy) AccentGreen
                            else if (busy) Color(0xFF1A1A1A)
                            else Color(0xFF1A1A1A),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            0.5.dp,
                            if (busy) AccentGreen.copy(0.3f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = inputText.isNotBlank() || busy) {
                            if (busy) {
                                vm.stopGeneration()
                            } else {
                                val text = inputText.trim()
                                inputText = ""
                                if (agentMode && activeProject != null) vm.runAgent(text)
                                else vm.sendMessage(text, vm.selectedImageUri.value)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (busy) {
                        Icon(Icons.Default.Stop, null, Modifier.size(16.dp), tint = AccentGreen)
                    } else {
                        Icon(
                            Icons.Default.ArrowUpward, null,
                            Modifier.size(17.dp),
                            tint = if (inputText.isNotBlank()) BgDeep else TextMuted
                        )
                    }
                }
            }

            // Bottom row: history + disclaimer
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { vm.refreshChatSessions(); showHistory = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.History, null, Modifier.size(15.dp), tint = TextMuted)
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    "Chat history",
                    color = TextMuted, fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Runs 100% on device",
                    color = TextMuted, fontSize = 10.sp
                )
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────
    if (showHistory) {
        ChatHistorySheet(
            sessions = chatSessions,
            onLoad = { vm.loadChatSession(it) },
            onDelete = { vm.deleteChatSession(it) },
            onDismiss = { showHistory = false }
        )
    }
    if (showSkills) {
        SkillsSheet(
            skills = skills,
            onSelect = { showSkills = false; selectedSkill = it },
            onDismiss = { showSkills = false }
        )
    }
    selectedSkill?.let { skill ->
        SkillVariablesDialog(
            skill = skill,
            extractVars = { vm.skills.value },
            skillsManager = null,
            onConfirm = { vars -> selectedSkill = null; vm.applySkill(skill, vars) },
            onDismiss = { selectedSkill = null }
        )
    }
}

// ── Claude-style message bubbles ──────────────────────────────────────────────

@Composable
fun AgentSummaryRow(steps: List<AgentStep>) {
    val successCount = steps.count { it.status == StepStatus.SUCCESS }
    val errorCount   = steps.count { it.status == StepStatus.ERROR }
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(8.dp))
            .border(0.5.dp, BgBorder, RoundedCornerShape(8.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(13.dp), tint = AccentGreen)
            Spacer(Modifier.width(6.dp))
            Text(
                "Agent completed · $successCount tools" +
                        if (errorCount > 0) " · $errorCount errors" else "",
                color = if (errorCount > 0) WarnYellow else AccentGreen,
                fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, Modifier.size(14.dp), tint = TextMuted
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(horizontal = 10.dp).padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEach { AgentStepCard(it) }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == MessageRole.USER

    if (isUser) {
        // User message — right aligned, green-tinted bubble like Claude
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .background(Color(0xFF1A3A2A), RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (msg.imageUri != null) {
                    Column {
                        AsyncImage(
                            model = msg.imageUri, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (msg.content.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(msg.content, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                    }
                } else {
                    Text(msg.content, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    } else {
        // Assistant message — left aligned, full width, no bubble — just like Claude
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // AI avatar
            Box(
                Modifier
                    .size(28.dp)
                    .background(Color(0xFF1A3A2A), CircleShape)
                    .border(0.5.dp, AccentGreen.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Memory, null, Modifier.size(14.dp), tint = AccentGreen)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                AssistantContent(msg)
                if (msg.tokensPerSecond > 0f && !msg.isStreaming) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${"%.1f".format(msg.tokensPerSecond)} tok/s",
                        color = TextMuted, fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantContent(msg: ChatMessage) {
    val segments = remember(msg.content) { parseMessageSegments(msg.content) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { seg ->
            when (seg) {
                is MessageSegment.Text -> {
                    if (seg.text.isNotBlank()) {
                        Text(
                            seg.text.trim(),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 23.sp
                        )
                    }
                }
                is MessageSegment.Code -> CodeBlock(language = seg.language, code = seg.code)
            }
        }
        if (msg.isStreaming) {
            Text("▋", color = AccentGreen, fontSize = 15.sp)
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFF0D1117), RoundedCornerShape(10.dp))
            .border(0.5.dp, BgBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(BgElevated, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(language.ifBlank { "code" }.lowercase(), color = TextMuted, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (copied) Text("Copied!", color = AccentGreen, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        null, Modifier.size(14.dp),
                        tint = if (copied) AccentGreen else TextMuted
                    )
                }
            }
        }
        Text(
            code.trimEnd(),
            color = Color(0xFFE6EDF3),
            fontSize = 12.sp, lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
    if (copied) {
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); copied = false }
    }
}

sealed class MessageSegment {
    data class Text(val text: String) : MessageSegment()
    data class Code(val language: String, val code: String) : MessageSegment()
}

fun parseMessageSegments(content: String): List<MessageSegment> {
    val result = mutableListOf<MessageSegment>()
    val regex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    var last = 0
    for (m in regex.findAll(content)) {
        if (m.range.first > last) result += MessageSegment.Text(content.substring(last, m.range.first))
        result += MessageSegment.Code(m.groupValues[1], m.groupValues[2])
        last = m.range.last + 1
    }
    if (last < content.length) result += MessageSegment.Text(content.substring(last))
    return result.ifEmpty { listOf(MessageSegment.Text(content)) }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    modelName: String?,
    onSkillsClick: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Large avatar
        Box(
            Modifier.size(64.dp)
                .background(Color(0xFF1A3A2A), CircleShape)
                .border(1.dp, AccentGreen.copy(0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Memory, null, Modifier.size(32.dp), tint = AccentGreen)
        }

        Spacer(Modifier.height(16.dp))
        Text("LocalLLM", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))

        if (modelName != null) {
            Text("Using $modelName", color = TextSecond, fontSize = 13.sp)
        } else {
            Row(
                Modifier
                    .background(Color(0xFF1A1000), RoundedCornerShape(8.dp))
                    .border(0.5.dp, WarnYellow.copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, Modifier.size(12.dp), tint = WarnYellow)
                Spacer(Modifier.width(6.dp))
                Text("No model loaded · go to Models tab", color = WarnYellow, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        // Suggestion grid
        val suggestions = listOf(
            "Generate a React todo app",
            "Fix a bug in my code",
            "Explain this function",
            "Write a Python script"
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            suggestions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s ->
                        Box(
                            Modifier.weight(1f)
                                .background(Color(0xFF141814), RoundedCornerShape(12.dp))
                                .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp))
                                .clickable { onSuggestion(s) }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(s, color = TextSecond, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSkillsClick,
            border = BorderStroke(0.5.dp, AccentGreen.copy(0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Browse Skills", fontSize = 13.sp)
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ── Skills / skill dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsSheet(skills: List<Skill>, onSelect: (Skill) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BgBorder) }
    ) {
        Text("Skills", style = MaterialTheme.typography.titleMedium, color = TextPrimary,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        val grouped = skills.groupBy { it.category }
        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            grouped.forEach { (cat, catSkills) ->
                item {
                    Text(cat.uppercase(), color = TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                items(catSkills) { skill ->
                    ListItem(
                        headlineContent = { Text("${skill.icon} ${skill.name}", color = TextPrimary, fontSize = 14.sp) },
                        supportingContent = { Text(skill.description, color = TextSecond, fontSize = 12.sp) },
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
        Regex("\\{\\{([A-Z_]+)\\}\\}").findAll(skill.userPromptTemplate)
            .map { it.groupValues[1] }.distinct().toList()
    }
    val values = remember { androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgSurface,
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
                            unfocusedContainerColor = BgElevated, focusedLabelColor = AccentGreen
                        ),
                        modifier = Modifier.fillMaxWidth(), maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(values.toMap()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep)) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecond) }
        }
    )
}
