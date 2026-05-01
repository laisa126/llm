package com.laiserdev.localllm.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.ui.theme.*

/**
 * Agent step cards — shown inline in ChatScreen while the agent loop runs.
 * Each tool call gets its own card with status, args, and collapsible output.
 * Inspired by Cursor / Lovable / Claude Code UX.
 */

data class AgentStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val args: String,
    val output: String = "",
    val status: StepStatus = StepStatus.RUNNING,
    val durationMs: Long = 0L
)

enum class StepStatus { RUNNING, SUCCESS, ERROR }

@Composable
fun AgentStepsPanel(
    steps: List<AgentStep>,
    thinking: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Thinking indicator
        AnimatedVisibility(
            visible = thinking != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (thinking != null) {
                ThinkingIndicator(thinking)
            }
        }

        // Step cards
        steps.forEach { step ->
            AgentStepCard(step)
        }
    }
}

@Composable
fun ThinkingIndicator(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "think")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1F17), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulsing dot
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = alpha))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            color = AccentCyan.copy(alpha = alpha + 0.1f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AgentStepCard(step: AgentStep) {
    var expanded by remember { mutableStateOf(step.status == StepStatus.ERROR) }

    val borderColor = when (step.status) {
        StepStatus.RUNNING -> AccentGreen
        StepStatus.SUCCESS -> BgBorder
        StepStatus.ERROR   -> ErrorRed
    }

    val bgColor = when (step.status) {
        StepStatus.RUNNING -> Color(0xFF0D1F17)
        StepStatus.SUCCESS -> BgSurface
        StepStatus.ERROR   -> Color(0xFF1F0D0D)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        // Header row — always visible
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { if (step.output.isNotBlank()) expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            when (step.status) {
                StepStatus.RUNNING -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val rotation by infiniteTransition.animateFloat(
                        0f, 360f,
                        infiniteRepeatable(tween(1000, easing = LinearEasing)),
                        label = "rot"
                    )
                    Icon(
                        Icons.Default.Refresh, null,
                        Modifier.size(14.dp).rotate(rotation),
                        tint = AccentGreen
                    )
                }
                StepStatus.SUCCESS ->
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = AccentGreen)
                StepStatus.ERROR ->
                    Icon(Icons.Default.Error, null, Modifier.size(14.dp), tint = ErrorRed)
            }

            Spacer(Modifier.width(8.dp))

            // Tool icon + name
            val (toolIcon, toolColor) = toolMeta(step.toolName)
            Text(toolIcon, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                step.toolName,
                color = toolColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.width(6.dp))

            // Args preview (truncated)
            Text(
                step.args.take(50).replace("\n", " ") + if (step.args.length > 50) "…" else "",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )

            // Duration
            if (step.durationMs > 0) {
                Text(
                    "${step.durationMs}ms",
                    color = TextMuted, fontSize = 10.sp
                )
                Spacer(Modifier.width(4.dp))
            }

            // Expand chevron
            if (step.output.isNotBlank()) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, Modifier.size(14.dp), tint = TextMuted
                )
            }
        }

        // Collapsible output
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (step.status == StepStatus.ERROR) Color(0xFF2A0D0D) else Color(0xFF0D1117),
                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(10.dp)
            ) {
                // Full args
                if (step.args.isNotBlank()) {
                    Text("INPUT", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        step.args,
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                    Spacer(Modifier.height(6.dp))
                }
                // Output
                if (step.output.isNotBlank()) {
                    Text("OUTPUT", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        step.output.take(800) + if (step.output.length > 800) "\n…(truncated)" else "",
                        color = if (step.status == StepStatus.ERROR) ErrorRed else TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

private fun toolMeta(name: String): Pair<String, Color> = when (name) {
    "read_file"       -> "📄" to Color(0xFF58A6FF)
    "write_file"      -> "✏️" to Color(0xFF3FB950)
    "create_file"     -> "📝" to Color(0xFF3FB950)
    "delete_file"     -> "🗑️" to Color(0xFFF85149)
    "list_files"      -> "📁" to Color(0xFFE3B341)
    "make_dir"        -> "📂" to Color(0xFFE3B341)
    "run_command"     -> "⚡" to Color(0xFFBC8CFF)
    "apply_diff"      -> "🔧" to Color(0xFF58A6FF)
    "grep_files"      -> "🔍" to Color(0xFF58A6FF)
    "get_file_info"   -> "ℹ️" to Color(0xFF8B949E)
    "install_package" -> "📦" to Color(0xFFE3B341)
    "search_web"      -> "🌐" to Color(0xFF58A6FF)
    "http_get"        -> "🌐" to Color(0xFF58A6FF)
    "list_processes"  -> "📊" to Color(0xFFBC8CFF)
    "kill_process"    -> "🛑" to Color(0xFFF85149)
    "download_zip"    -> "📥" to Color(0xFF00D4FF)
    "screenshot"      -> "📸" to Color(0xFF9B6DFF)
    else              -> "🔧" to Color(0xFF8B949E)
}
