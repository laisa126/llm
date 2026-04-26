package com.laiserdev.localllm.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.ui.theme.*

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(BgDeep)) {
        when (step) {
            0 -> WelcomeStep(onNext = { step = 1 })
            1 -> HowItWorksStep(onNext = { step = 2 })
            2 -> DownloadStep(onDone = onDone)
        }
        // Step dots
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { i ->
                Box(
                    Modifier.size(if (i == step) 20.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == step) AccentGreen else BgBorder)
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
        Box(Modifier.size(88.dp).background(Color(0xFF1A3A2A), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Memory, null, Modifier.size(48.dp), tint = AccentGreen)
        }
        Spacer(Modifier.height(28.dp))
        Text("LocalLLM", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("A full AI coding assistant that runs\n100% on your device. No internet required.",
            color = TextSecond, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(40.dp))
        FeatureRow(Icons.Default.Lock, "Private", "Your code never leaves your phone")
        Spacer(Modifier.height(14.dp))
        FeatureRow(Icons.Default.Code, "Full IDE", "Editor, terminal, preview, agent mode")
        Spacer(Modifier.height(14.dp))
        FeatureRow(Icons.Default.SmartToy, "Agentic", "AI reads, writes and runs your code")
        Spacer(Modifier.height(14.dp))
        FeatureRow(Icons.Default.Api, "Local API", "OpenAI-compatible REST API on device")
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
            shape = RoundedCornerShape(14.dp)) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HowItWorksStep(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 60.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("How it works", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Three steps to your local AI assistant", color = TextSecond, fontSize = 14.sp)
        Spacer(Modifier.height(36.dp))
        StepCard("1","Download a Model","Go to Models tab. Tap Download on Gemma 3 1B (700MB). Needs Wi-Fi.",Icons.Default.Download, AccentGreen)
        Spacer(Modifier.height(12.dp))
        StepCard("2","Load the Model","After download, tap Load Model. First load takes ~10 seconds.",Icons.Default.PlayArrow, AccentBlue)
        Spacer(Modifier.height(12.dp))
        StepCard("3","Start Coding","Chat with AI, use Agent mode to build projects, or use the local API.",Icons.Default.Chat, Color(0xFFBC8CFF))
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1500)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, WarnYellow),
            shape = RoundedCornerShape(10.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = WarnYellow)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("RAM Requirements", color = WarnYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("• Gemma 3 1B — 3GB+ RAM  (start here)\n• Gemma 3 4B — 6GB+ RAM\n• Gemma 3n E4B — 8GB+ RAM\n\nLoading a model too large for your RAM will crash the app.",
                        color = TextSecond, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(36.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
            shape = RoundedCornerShape(14.dp)) {
            Text("Go to Models", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Memory, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DownloadStep(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 60.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(72.dp).background(Color(0xFF1A3A2A), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(38.dp), tint = AccentGreen)
        }
        Spacer(Modifier.height(24.dp))
        Text("You're all set!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Head to the Models tab, download\nGemma 3 1B ⭐, then tap Load Model.",
            color = TextSecond, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(40.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgDeep),
            shape = RoundedCornerShape(14.dp)) {
            Text("Open LocalLLM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().background(BgSurface, RoundedCornerShape(10.dp))
        .border(0.5.dp, BgBorder, RoundedCornerShape(10.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(BgElevated, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = AccentGreen)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StepCard(number: String, title: String, description: String, icon: ImageVector, color: Color) {
    Row(Modifier.fillMaxWidth().background(BgSurface, RoundedCornerShape(12.dp))
        .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.Top) {
        Box(Modifier.size(32.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center) {
            Text(number, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(14.dp), tint = color)
                Spacer(Modifier.width(6.dp))
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(description, color = TextSecond, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
