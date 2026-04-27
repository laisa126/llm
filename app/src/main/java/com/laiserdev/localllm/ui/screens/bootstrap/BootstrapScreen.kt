package com.laiserdev.localllm.ui.screens.bootstrap

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.ui.theme.*

@Composable
fun BootstrapScreen(
    progress: Float,          // 0.0 .. 1.0
    phase: BootstrapPhase
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rot"
    )

    Box(
        Modifier.fillMaxSize().background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            // Animated logo
            Box(
                Modifier
                    .size(88.dp)
                    .background(Color(0xFF0D1F0D), CircleShape)
                    .border(1.5.dp, AccentGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (phase == BootstrapPhase.DONE) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        Modifier.size(44.dp), tint = AccentGreen
                    )
                } else {
                    Icon(
                        Icons.Default.Memory, null,
                        Modifier.size(44.dp).rotate(if (phase == BootstrapPhase.EXTRACTING) rotation else 0f),
                        tint = AccentGreen
                    )
                }
            }

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "LocalLLM",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when (phase) {
                        BootstrapPhase.CHECKING   -> "Checking AI model…"
                        BootstrapPhase.EXTRACTING -> "Setting up AI model…"
                        BootstrapPhase.LOADING    -> "Loading AI model…"
                        BootstrapPhase.DONE       -> "Ready!"
                        BootstrapPhase.ERROR      -> "Setup failed"
                    },
                    color = TextSecond,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Progress bar + percentage
            if (phase == BootstrapPhase.EXTRACTING || phase == BootstrapPhase.LOADING) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(BgSurface, RoundedCornerShape(12.dp))
                        .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (phase == BootstrapPhase.EXTRACTING)
                                "Extracting Gemma 3 1B"
                            else "Loading into memory",
                            color = TextSecond, fontSize = 13.sp
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AccentGreen,
                        trackColor = BgElevated,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        if (phase == BootstrapPhase.EXTRACTING)
                            "~700MB · One-time setup, takes ~30 seconds"
                        else
                            "First load takes a few seconds…",
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }

            // Loading spinner for checking/loading phases
            if (phase == BootstrapPhase.CHECKING || phase == BootstrapPhase.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = AccentGreen,
                    strokeWidth = 2.5.dp
                )
            }

            // Done state
            if (phase == BootstrapPhase.DONE) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D2A1A), RoundedCornerShape(12.dp))
                        .border(0.5.dp, AccentGreen.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = AccentGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Gemma 3 1B ready", color = AccentGreen, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = TextMuted)
                        Spacer(Modifier.width(8.dp))
                        Text("Runs 100% on-device", color = TextSecond, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, null, Modifier.size(14.dp), tint = TextMuted)
                        Spacer(Modifier.width(8.dp))
                        Text("Agent mode enabled", color = TextSecond, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

enum class BootstrapPhase { CHECKING, EXTRACTING, LOADING, DONE, ERROR }
