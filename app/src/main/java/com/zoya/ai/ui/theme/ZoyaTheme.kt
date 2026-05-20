package com.zoya.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Colors
@Immutable
data class ZoyaColors(
    val background: Color = Color(0xFF0A0A0F),
    val dark: Color = Color(0xFF0D0D15),
    val card: Color = Color(0xFF12121C),
    val cardAlpha: Color = Color(0x9912121C),
    val border: Color = Color(0xFF1A1A2E),
    val accent: Color = Color(0xFFA855F7),
    val accent2: Color = Color(0xFFEC4899),
    val glow: Color = Color(0xFFC084FC),
    val text: Color = Color(0xFFE2E8F0),
    val muted: Color = Color(0xFF64748B),
    val green: Color = Color(0xFF22C55E),
    val white: Color = Color.White,
    val error: Color = Color(0xFFEF4444)
)

val ZoyaGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C3AED),
        Color(0xFFA855F7),
        Color(0xFFEC4899)
    )
)

val ZoyaGradientVertical = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFA855F7),
        Color(0xFFEC4899)
    )
)

val ZoyaButtonGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C3AED),
        Color(0xFFEC4899)
    )
)

val ZoyaButtonIdleGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1E1E2E),
        Color(0xFF2A2A40)
    )
)

val LocalZoyaColors = staticCompositionLocalOf { ZoyaColors() }

object ZoyaTheme {
    val colors: ZoyaColors
        @Composable
        get() = LocalZoyaColors.current
}

@Composable
fun ZoyaTheme(
    content: @Composable () -> Unit
) {
    val colors = ZoyaColors()
    
    CompositionLocalProvider(
        LocalZoyaColors provides colors,
        content = content
    )
}
