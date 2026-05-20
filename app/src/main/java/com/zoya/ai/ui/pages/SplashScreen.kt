package com.zoya.ai.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.ui.theme.ZoyaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    var lightY by remember { mutableFloatStateOf(-300f) }
    var logoAlpha by remember { mutableFloatStateOf(0f) }
    var logoScale by remember { mutableFloatStateOf(0.3f) }
    var textAlpha by remember { mutableFloatStateOf(0f) }
    var subtitleAlpha by remember { mutableFloatStateOf(0f) }
    var ringAlpha by remember { mutableFloatStateOf(0f) }
    var ringScale by remember { mutableFloatStateOf(1.5f) }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        // Phase 0: Light streak from top
        delay(200)
        animate(0f, 1f, animationSpec = tween(800, easing = EaseOutCubic)) { value, _ ->
            lightY = -300f + (300f * value)
        }
        delay(300)

        // Phase 1: Logo appears
        phase = 1
        launch {
            animate(0f, 1f, animationSpec = tween(600, easing = EaseOutBack)) { value, _ ->
                logoAlpha = value
                logoScale = 0.3f + (0.7f * value)
            }
        }
        delay(200)

        // Ring animation
        launch {
            animate(0f, 1f, animationSpec = tween(500, easing = EaseOutCubic)) { value, _ ->
                ringAlpha = value * 0.6f
                ringScale = 1.5f - (0.5f * value)
            }
        }
        delay(400)

        // Phase 2: Text appears
        animate(0f, 1f, animationSpec = tween(400)) { value, _ ->
            textAlpha = value
        }
        delay(200)
        animate(0f, 1f, animationSpec = tween(400)) { value, _ ->
            subtitleAlpha = value
        }
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(ZoyaTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Background radial glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(ringScale)
                .alpha(ringAlpha * glowPulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.3f),
                            Color(0xFFA855F7).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Light streak from top
        if (phase == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .offset(y = lightY.dp)
                    .alpha(0.8f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFFA855F7),
                                Color(0xFFEC4899),
                                Color(0xFFA855F7),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Expanding rings
        if (phase >= 1) {
            for (i in 0..2) {
                val ringInfinite = rememberInfiniteTransition(label = "ring$i")
                val ringPulse by ringInfinite.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(1000 + i * 200, easing = EaseInOutSine),
                        RepeatMode.Reverse
                    ),
                    label = "rp$i"
                )
                Box(
                    modifier = Modifier
                        .size((100 + i * 40).dp)
                        .scale(ringPulse)
                        .alpha(ringAlpha * (0.4f - i * 0.1f))
                        .clip(CircleShape)
                        .background(Color.Transparent)
                )
            }
        }

        // Main content column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = if (phase >= 1) 0.dp else lightY.dp)
        ) {
            // Logo icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF7C3AED),
                                Color(0xFFA855F7),
                                Color(0xFFEC4899)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎙️", fontSize = 36.sp)
            }

            if (phase >= 1) {
                Spacer(Modifier.height(20.dp))

                Text(
                    "ZOYA",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZoyaTheme.colors.accent,
                    letterSpacing = 6.sp,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "AI Voice Assistant",
                    fontSize = 14.sp,
                    color = ZoyaTheme.colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(subtitleAlpha)
                )

                Spacer(Modifier.height(30.dp))

                LoadingDots(subtitleAlpha)
            }
        }
    }
}

@Composable
private fun LoadingDots(parentAlpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(parentAlpha)
    ) {
        for (i in 0..2) {
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ZoyaTheme.colors.accent.copy(alpha = dotAlpha))
            )
        }
    }
}
