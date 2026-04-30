package com.laiserdev.localllm.ui.screens.onboarding

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.R
import com.laiserdev.localllm.ui.theme.*

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.radialGradient(listOf(Color(0xFF0A1220), BgDeep), radius = 1400f)
            )
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it / 3 } + fadeIn() togetherWith
                slideOutHorizontally { -it / 3 } + fadeOut()
            },
            label = "step"
        ) { s ->
            when (s) {
                0 -> WelcomeStep(onNext = { step = 1 })
                1 -> FeaturesStep(onNext = { step = 2 })
                2 -> ReadyStep(onDone = onDone)
            }
        }

        // Step dots
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val w by animateDpAsState(if (i == step) 24.dp else 6.dp, label = "dot")
                Box(
                    Modifier.size(w, 6.dp).clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i == step)
                                Brush.horizontalGradient(listOf(AccentCyan, AccentPurple))
                            else
                                Brush.horizontalGradient(listOf(BgBorderBright, BgBorderBright))
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp).padding(top = 80.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(26.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(24.dp))

        Text(
            "Local LLM Agent",
            fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "A full AI coding assistant that runs\n100% on your phone. No internet. No cloud.",
            color = TextSecond, fontSize = 15.sp,
            textAlign = TextAlign.Center, lineHeight = 23.sp
        )
        Spacer(Modifier.height(36.dp))

        // Feature pills
        FeaturePill(Icons.Default.Lock, "Private", "Your code never leaves your phone", AccentCyan)
        Spacer(Modifier.height(12.dp))
        FeaturePill(Icons.Default.Code, "Full IDE", "Editor, terminal, preview, agent mode", AccentPurple)
        Spacer(Modifier.height(12.dp))
        FeaturePill(Icons.Default.SmartToy, "Agentic", "AI reads, writes and runs your code", AccentBlue)
        Spacer(Modifier.height(12.dp))
        FeaturePill(Icons.Default.Api, "Local API", "OpenAI-compatible REST on device", AccentGreen)

        Spacer(Modifier.height(44.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(AccentCyan, AccentPurple))),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(
                            AccentCyan.copy(0.15f), AccentPurple.copy(0.15f)
                        )),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Get Started", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = AccentCyan)
                }
            }
        }
    }
}

@Composable
private fun FeaturesStep(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 80.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp)
                .background(
                    Brush.radialGradient(listOf(AccentCyan.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
                .border(1.dp, AccentCyan.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(32.dp), tint = AccentCyan)
        }
        Spacer(Modifier.height(20.dp))
        Text("How it works", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Everything runs locally on your device",
            color = TextSecond, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        val steps = listOf(
            Triple(Icons.Default.Download, "Download a model",
                "Get Gemma 3 1B (built-in, ~700MB) from the Models tab"),
            Triple(Icons.Default.FolderOpen, "Open a project",
                "Create or import a project in the Editor tab"),
            Triple(Icons.Default.SmartToy, "Activate agent mode",
                "Toggle Agent in Chat — AI will read, write, and run your code"),
            Triple(Icons.Default.Api, "Use the local API",
                "Access via localhost:8080 — OpenAI-compatible for other tools")
        )
        steps.forEachIndexed { i, (icon, title, desc) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(36.dp)
                        .background(BgSurface, CircleShape)
                        .border(0.5.dp, BgBorderBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(16.dp), tint = AccentCyan)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(desc, color = TextSecond, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            if (i < steps.size - 1) {
                Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(1.dp).height(16.dp).background(BgBorder))
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BgDeep)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = BgDeep)
        }
    }
}

@Composable
private fun ReadyStep(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp).padding(top = 80.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(110.dp)
                    .background(
                        Brush.radialGradient(listOf(AccentGreen.copy(0.2f), Color.Transparent)),
                        CircleShape
                    )
            )
            Icon(Icons.Default.CheckCircle, null,
                Modifier.size(64.dp), tint = AccentGreen)
        }
        Spacer(Modifier.height(22.dp))
        Text("You're all set", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text(
            "The built-in Gemma 3 1B model is ready.\nGo to Models to download larger models.",
            color = TextSecond, fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Start Coding", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BgDeep)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Code, null, Modifier.size(18.dp), tint = BgDeep)
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector, title: String, subtitle: String, accent: Color
) {
    Row(
        Modifier.fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(12.dp))
            .border(0.5.dp, accent.copy(0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp)
                .background(accent.copy(0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextSecond, fontSize = 12.sp)
        }
    }
}

