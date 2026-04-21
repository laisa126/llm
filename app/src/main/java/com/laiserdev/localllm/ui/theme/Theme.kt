package com.laiserdev.localllm.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Color palette — VSCode-inspired dark theme ───────────────────────────────

val BgDeep       = Color(0xFF0D1117)   // GitHub dark bg
val BgSurface    = Color(0xFF161B22)   // cards / panels
val BgElevated   = Color(0xFF21262D)   // inputs / hover
val BgBorder     = Color(0xFF30363D)   // borders

val AccentGreen  = Color(0xFF3FB950)   // primary CTA
val AccentBlue   = Color(0xFF58A6FF)   // links / active
val AccentPurple = Color(0xFFBC8CFF)   // assistant messages
val AccentOrange = Color(0xFFE3B341)   // warnings

val TextPrimary  = Color(0xFFE6EDF3)
val TextSecond   = Color(0xFF8B949E)
val TextMuted    = Color(0xFF484F58)

val ErrorRed     = Color(0xFFF85149)
val SuccessGreen = Color(0xFF3FB950)
val WarnYellow   = Color(0xFFE3B341)

private val DarkColors = darkColorScheme(
    primary          = AccentGreen,
    onPrimary        = BgDeep,
    primaryContainer = Color(0xFF1A3A2A),
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
fun LocalLLMTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}

// Monospace font for code
val MonoFont = FontFamily.Monospace

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        color = TextPrimary
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        color = TextSecond
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontFamily = MonoFont,
        color = TextMuted
    )
)

// Language → syntax color
fun extensionColor(ext: String): Color = when (ext.lowercase()) {
    "kt", "kts"           -> Color(0xFF7F52FF)
    "js", "jsx", "mjs"    -> Color(0xFFF7DF1E)
    "ts", "tsx"           -> Color(0xFF3178C6)
    "py"                  -> Color(0xFF3776AB)
    "html", "htm"         -> Color(0xFFE34F26)
    "css", "scss", "sass" -> Color(0xFF264DE4)
    "json"                -> Color(0xFFCBCB41)
    "md", "mdx"           -> Color(0xFF083FA1)
    "sh", "bash", "zsh"   -> Color(0xFF4EAA25)
    "xml"                 -> Color(0xFFFF6600)
    "java"                -> Color(0xFFB07219)
    "c", "cpp", "h"       -> Color(0xFF555555)
    "rs"                  -> Color(0xFFDEA584)
    "go"                  -> Color(0xFF00ADD8)
    "rb"                  -> Color(0xFFCC342D)
    "php"                 -> Color(0xFF4F5D95)
    "swift"               -> Color(0xFFFA7343)
    "dart"                -> Color(0xFF00B4AB)
    else                  -> TextSecond
}
