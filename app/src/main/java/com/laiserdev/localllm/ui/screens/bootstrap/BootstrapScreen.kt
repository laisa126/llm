package com.laiserdev.localllm.ui.screens.bootstrap

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.R
import com.laiserdev.localllm.ui.theme.*

@Composable
fun BootstrapScreen(progress: Float, phase: BootstrapPhase) {

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse),
        label = "scale"
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse),
        label = "ring"
    )

    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0A1525), BgDeep),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {

            // ── Pulsing logo ──────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AccentCyan.copy(alpha = ringAlpha),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                // Icon
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "LocalLLM",
                    modifier = Modifier
                        .size(88.dp)
                        .scale(if (phase == BootstrapPhase.LOADING) scale else 1f)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Fit
                )
                // Done check overlay
                if (phase == BootstrapPhase.DONE) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(AccentGreen, CircleShape)
                            .border(2.dp, BgDeep, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            Modifier.size(16.dp), tint = BgDeep)
                    }
                }
            }

            // ── Title ─────────────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Local LLM Agent",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when (phase) {
                        BootstrapPhase.CHECKING   -> "Checking AI model…"
                        BootstrapPhase.EXTRACTING -> "Setting up Gemma 3 1B…"
                        BootstrapPhase.LOADING    -> "Loading into memory…"
                        BootstrapPhase.DONE       -> "Ready to code"
                        BootstrapPhase.ERROR      -> "Setup encountered an error"
                    },
                    color = if (phase == BootstrapPhase.DONE) AccentCyan else TextSecond,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // ── Progress ──────────────────────────────────────────────────────
            if (phase == BootstrapPhase.EXTRACTING || phase == BootstrapPhase.LOADING) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(BgSurface, RoundedCornerShape(14.dp))
                        .border(0.5.dp, BgBorderBright, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (phase == BootstrapPhase.EXTRACTING) "Extracting model"
                            else "Loading model",
                            color = TextSecond, fontSize = 12.sp
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            color = AccentCyan, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentCyan,
                        trackColor = BgElevated,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "~700MB · One-time setup",
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }

            if (phase == BootstrapPhase.CHECKING || phase == BootstrapPhase.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp
                )
            }

            // ── Done card ─────────────────────────────────────────────────────
            if (phase == BootstrapPhase.DONE) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF002A1A), Color(0xFF00152A))
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .border(0.5.dp,
                            Brush.horizontalGradient(listOf(AccentGreen.copy(0.4f), AccentCyan.copy(0.4f))),
                            RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        Modifier.size(15.dp), tint = AccentGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Gemma 3 1B · 100% on-device",
                        color = AccentGreen, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

enum class BootstrapPhase { CHECKING, EXTRACTING, LOADING, DONE, ERROR }
