package com.laiserdev.localllm.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Refined color system ─────────────────────────────────────────────────────

val BgDeep       = Color(0xFF080C10)   // Almost black — deepest bg
val BgSurface    = Color(0xFF0F1419)   // Cards, panels
val BgElevated   = Color(0xFF161D26)   // Inputs, hover states
val BgBorder     = Color(0xFF1E2733)   // Subtle borders
val BgBorderBright = Color(0xFF2A3A4A) // Active borders

val AccentCyan   = Color(0xFF00D4FF)   // Primary — electric cyan (matches your icon)
val AccentBlue   = Color(0xFF4D9FFF)   // Secondary — softer blue
val AccentPurple = Color(0xFF9B6DFF)   // Tertiary — purple (matches icon gradient)
val AccentGreen  = Color(0xFF00E5A0)   // Success / confirm
val AccentOrange = Color(0xFFFF8C42)   // Warning / file folders

val TextPrimary  = Color(0xFFEAF0F8)
val TextSecond   = Color(0xFF7A8FA8)
val TextMuted    = Color(0xFF3D5068)

val ErrorRed     = Color(0xFFFF4D6A)
val SuccessGreen = Color(0xFF00E5A0)
val WarnYellow   = Color(0xFFFFCC44)

// Gradient stops matching the icon
val GradCyanStart  = Color(0xFF00C8FF)
val GradPurpleEnd  = Color(0xFF9B6DFF)

private val DarkColors = darkColorScheme(
    primary          = AccentCyan,
    onPrimary        = BgDeep,
    primaryContainer = Color(0xFF00243A),
    secondary        = AccentBlue,
    onSecondary      = BgDeep,
    tertiary         = AccentPurple,
    background       = BgDeep,
    onBackground     = TextPrimary,
    surface          = BgSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BgElevated,
    onSurfaceVariant = TextSecond,
    outline          = BgBorder,
    error            = ErrorRed,
    onError          = Color.White
)

@Composable
fun LocalLLMTheme(
    fontFamily: String = "sans",
    fontSize: Int = 14,
    content: @Composable () -> Unit
) {
    val ff = when (fontFamily) {
        "serif" -> FontFamily.Serif
        "mono"  -> FontFamily.Monospace
        else    -> FontFamily.Default
    }
    val bodySize = fontSize.sp
    val typography = Typography(
        headlineLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,   fontSize = 24.sp,       color = TextPrimary),
        headlineMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,     color = TextPrimary),
        titleMedium    = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium, fontSize = 16.sp,       color = TextPrimary),
        bodyLarge      = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = bodySize,    color = TextPrimary),
        bodyMedium     = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = bodySize,    color = TextPrimary),
        bodySmall      = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = (fontSize - 2).sp, color = TextSecond),
        labelSmall     = TextStyle(fontFamily = MonoFont,                           fontSize = 11.sp,       color = TextMuted)
    )
    MaterialTheme(
        colorScheme = DarkColors,
        typography = typography,
        content = content
    )
}

val MonoFont = FontFamily.Monospace

val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, color = TextPrimary),
    bodySmall = TextStyle(fontSize = 12.sp, color = TextSecond),
    labelSmall = TextStyle(fontSize = 11.sp, fontFamily = MonoFont, color = TextMuted)
)

fun extensionColor(ext: String): Color = when (ext.lowercase()) {
    "kt", "kts"           -> Color(0xFF9B6DFF)
    "js", "jsx", "mjs"    -> Color(0xFFF7DF1E)
    "ts", "tsx"           -> Color(0xFF4D9FFF)
    "py"                  -> Color(0xFF3776AB)
    "html", "htm"         -> Color(0xFFFF6550)
    "css", "scss", "sass" -> Color(0xFF4D9FFF)
    "json"                -> Color(0xFFFFCC44)
    "md", "mdx"           -> Color(0xFF00D4FF)
    "sh", "bash", "zsh"   -> Color(0xFF00E5A0)
    "xml"                 -> Color(0xFFFF8C42)
    "java"                -> Color(0xFFFF8C42)
    "c", "cpp", "h"       -> Color(0xFF7A8FA8)
    "rs"                  -> Color(0xFFFF8C42)
    "go"                  -> Color(0xFF00D4FF)
    "rb"                  -> Color(0xFFFF4D6A)
    "dart"                -> Color(0xFF00D4FF)
    else                  -> TextSecond
}
