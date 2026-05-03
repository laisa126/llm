package com.laiserdev.localllm.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.ui.theme.*

data class AgentStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val args: String,
    val output: String = "",
    val status: StepStatus = StepStatus.RUNNING,
    val durationMs: Long = 0L
)

enum class StepStatus { RUNNING, SUCCESS, ERROR }

// ── Tool icon/color registry — Material icons only ───────────────────────────
private data class ToolMeta(val icon: ImageVector, val color: Color, val label: String)

private fun toolMeta(name: String): ToolMeta = when (name) {
    "read_file"       -> ToolMeta(Icons.Outlined.Description,     Color(0xFF58A6FF), "Read")
    "write_file"      -> ToolMeta(Icons.Outlined.Edit,            Color(0xFF3FB950), "Write")
    "create_file"     -> ToolMeta(Icons.Outlined.NoteAdd,         Color(0xFF3FB950), "Create")
    "delete_file"     -> ToolMeta(Icons.Outlined.DeleteOutline,   Color(0xFFF85149), "Delete")
    "list_files"      -> ToolMeta(Icons.Outlined.FolderOpen,      Color(0xFFE3B341), "List")
    "make_dir"        -> ToolMeta(Icons.Outlined.CreateNewFolder, Color(0xFFE3B341), "Mkdir")
    "run_command"     -> ToolMeta(Icons.Outlined.Terminal,        Color(0xFFBC8CFF), "Run")
    "apply_diff"      -> ToolMeta(Icons.Outlined.Difference,      Color(0xFF58A6FF), "Diff")
    "grep_files"      -> ToolMeta(Icons.Outlined.Search,          Color(0xFF58A6FF), "Search")
    "get_file_info"   -> ToolMeta(Icons.Outlined.Info,            Color(0xFF8B949E), "Info")
    "install_package" -> ToolMeta(Icons.Outlined.Extension,       Color(0xFFE3B341), "Install")
    "search_web"      -> ToolMeta(Icons.Outlined.Language,        Color(0xFF58A6FF), "Web")
    "http_get"        -> ToolMeta(Icons.Outlined.Http,            Color(0xFF58A6FF), "HTTP")
    "list_processes"  -> ToolMeta(Icons.Outlined.Memory,          Color(0xFFBC8CFF), "Processes")
    "kill_process"    -> ToolMeta(Icons.Outlined.Stop,            Color(0xFFF85149), "Kill")
    "download_zip"    -> ToolMeta(Icons.Outlined.FolderZip,       Color(0xFF00D4FF), "Zip")
    "screenshot"      -> ToolMeta(Icons.Outlined.Screenshot,      Color(0xFF9B6DFF), "Screenshot")
    else              -> ToolMeta(Icons.Outlined.Build,           Color(0xFF8B949E), name)
}

// ── Thinking indicator — Claude-style pulsing dots ────────────────────────────
@Composable
fun ThinkingIndicator(message: String) {
    val transition = rememberInfiniteTransition(label = "think")
    val dot1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, 0), RepeatMode.Reverse), label = "d1")
    val dot2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, 150), RepeatMode.Reverse), label = "d2")
    val dot3 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, 300), RepeatMode.Reverse), label = "d3")

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgSurface)
            .border(0.5.dp, BgBorderBright, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Three pulsing dots (Claude-style)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(dot1, dot2, dot3).forEach { alpha ->
                Box(Modifier.size(5.dp).clip(CircleShape)
                    .background(AccentCyan.copy(alpha = alpha)))
            }
        }
        Text(
            message.ifBlank { "Thinking…" },
            color = TextSecond,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Agent steps panel ─────────────────────────────────────────────────────────
@Composable
fun AgentStepsPanel(
    steps: List<AgentStep>,
    thinking: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgSurface)
            .border(0.5.dp, BgBorderBright, RoundedCornerShape(10.dp)),
    ) {
        // Header — like Claude's "Using tool" header
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(AccentCyan.copy(0.06f), Color.Transparent))
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val spin = rememberInfiniteTransition(label = "spin")
            val rot by spin.animateFloat(0f, 360f,
                infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "r")
            Icon(
                Icons.Default.AutoAwesome, null,
                Modifier.size(13.dp).let { if (thinking != null) it.rotate(rot) else it },
                tint = AccentCyan
            )
            Text(
                if (thinking != null) "Thinking…" else "Agent",
                color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${steps.size} step${if (steps.size != 1) "s" else ""}",
                color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
        }

        HorizontalDivider(color = BgBorderBright, thickness = 0.5.dp)

        // Thinking block
        AnimatedVisibility(thinking != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            if (thinking != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val t2 = rememberInfiniteTransition(label = "t")
                    val d1 by t2.animateFloat(0.2f,1f,infiniteRepeatable(tween(600,0),RepeatMode.Reverse),"d1")
                    val d2 by t2.animateFloat(0.2f,1f,infiniteRepeatable(tween(600,150),RepeatMode.Reverse),"d2")
                    val d3 by t2.animateFloat(0.2f,1f,infiniteRepeatable(tween(600,300),RepeatMode.Reverse),"d3")
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(d1,d2,d3).forEach { a -> Box(Modifier.size(4.dp).clip(CircleShape).background(AccentCyan.copy(a))) }
                    }
                    Text(thinking, color = TextSecond, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = BgBorderBright, thickness = 0.5.dp)
            }
        }

        // Step list
        steps.forEachIndexed { i, step ->
            AgentStepRow(step)
            if (i < steps.size - 1) HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
        }
    }
}

// ── Single step row — Claude Code style ───────────────────────────────────────
@Composable
fun AgentStepCard(step: AgentStep) = AgentStepRow(step)

@Composable
private fun AgentStepRow(step: AgentStep) {
    var expanded by remember { mutableStateOf(step.status == StepStatus.ERROR) }
    val meta = toolMeta(step.toolName)

    Column(Modifier.fillMaxWidth()) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = step.output.isNotBlank()) { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status icon — left side
            StatusIcon(step.status)

            // Tool type icon
            Icon(meta.icon, null, Modifier.size(14.dp), tint = meta.color)

            // Tool name
            Text(
                step.toolName,
                color = meta.color,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            // Args preview — grayed out, like Claude's tool param preview
            Text(
                step.args.take(48).replace("\n", " ").let {
                    if (step.args.length > 48) "$it…" else it
                },
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Duration badge
            if (step.durationMs > 0) {
                Text(
                    "${step.durationMs}ms",
                    color = TextMuted, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Expand chevron
            if (step.output.isNotBlank()) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, Modifier.size(15.dp), tint = TextMuted
                )
            }
        }

        // ── Collapsible detail — like Claude's tool input/output blocks ───────
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (step.status == StepStatus.ERROR)
                            Color(0xFF1A0808) else Color(0xFF080D14)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Input section
                if (step.args.isNotBlank()) {
                    DetailSection("Input", AccentBlue) {
                        Text(
                            step.args,
                            color = AccentBlue.copy(0.85f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
                // Output section
                if (step.output.isNotBlank()) {
                    val outColor = if (step.status == StepStatus.ERROR) ErrorRed else TextPrimary
                    DetailSection(
                        if (step.status == StepStatus.ERROR) "Error" else "Output",
                        if (step.status == StepStatus.ERROR) ErrorRed else AccentGreen
                    ) {
                        Text(
                            step.output.take(800) + if (step.output.length > 800) "\n…(truncated)" else "",
                            color = outColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(label: String, accentColor: Color, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(3.dp, 10.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
            Text(label.uppercase(), color = accentColor, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        content()
    }
}

@Composable
private fun StatusIcon(status: StepStatus) {
    when (status) {
        StepStatus.RUNNING -> {
            val spin = rememberInfiniteTransition(label = "spin")
            val rot by spin.animateFloat(0f, 360f,
                infiniteRepeatable(tween(800, easing = LinearEasing)), label = "r")
            Icon(Icons.Default.Sync, null,
                Modifier.size(14.dp).rotate(rot), tint = AccentCyan)
        }
        StepStatus.SUCCESS ->
            Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = AccentGreen)
        StepStatus.ERROR ->
            Icon(Icons.Default.Error, null, Modifier.size(14.dp), tint = ErrorRed)
    }
}
