package com.zoya.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.data.SessionState
import com.zoya.ai.ui.theme.*

@Composable
fun PowerButton(
    state: SessionState,
    onClick: () -> Unit
) {
    val isActive = state != SessionState.DISCONNECTED
    val isConnecting = state == SessionState.CONNECTING
    val isSpeaking = state == SessionState.SPEAKING
    
    val infiniteTransition = rememberInfiniteTransition(label = "button")
    
    // Rotation for outer rings
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )
    
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )
    
    // Pulse for ripple
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )
    
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )
    
    // Glow pulse
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // Outer rotating rings (only when active)
        if (isActive) {
            // Ring 1
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .rotate(rotation1)
                    .border(
                        width = 1.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ZoyaTheme.colors.accent,
                                Color.Transparent,
                                ZoyaTheme.colors.accent.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Ring 2
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(rotation2)
                    .border(
                        width = 1.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                ZoyaTheme.colors.accent2,
                                Color.Transparent,
                                ZoyaTheme.colors.accent2.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Ripple effect
            if (!isConnecting) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(rippleScale)
                        .border(
                            width = 2.dp,
                            color = (if (isSpeaking) ZoyaTheme.colors.accent2 else ZoyaTheme.colors.accent)
                                .copy(alpha = rippleAlpha),
                            shape = CircleShape
                        )
                )
            }
        }
        
        // Glow background
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(if (isActive) glowScale else 1f)
                .background(
                    Brush.radialGradient(
                        colors = if (isActive) {
                            if (isSpeaking) {
                                listOf(
                                    Color(0x33EC4899),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0x33A855F7),
                                    Color.Transparent
                                )
                            }
                        } else {
                            listOf(
                                Color(0x0DA855F7),
                                Color.Transparent
                            )
                        }
                    ),
                    CircleShape
                )
        )
        
        // Main button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .shadow(
                    elevation = if (isActive) 20.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isSpeaking) ZoyaTheme.colors.accent2 else ZoyaTheme.colors.accent,
                    spotColor = if (isSpeaking) ZoyaTheme.colors.accent2 else ZoyaTheme.colors.accent
                )
                .clip(CircleShape)
                .background(
                    if (isActive) {
                        if (isSpeaking) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF9333EA),
                                    Color(0xFFEC4899),
                                    Color(0xFF8B5CF6)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF7C3AED),
                                    Color(0xFFA855F7),
                                    Color(0xFF6366F1)
                                )
                            )
                        }
                    } else {
                        ZoyaButtonIdleGradient
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (isActive) {
                        ZoyaTheme.colors.accent.copy(alpha = 0.4f)
                    } else {
                        ZoyaTheme.colors.accent.copy(alpha = 0.15f)
                    },
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isConnecting,
                    onClick = onClick
                )
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else if (isActive) {
                // Microphone icon
                Text(
                    text = "🎙️",
                    fontSize = 36.sp
                )
            } else {
                // Power icon
                Text(
                    text = "⚡",
                    fontSize = 36.sp
                )
            }
        }
        
        // Inner ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(145.dp)
                    .border(
                        width = 1.dp,
                        color = if (isSpeaking) {
                            ZoyaTheme.colors.accent2.copy(alpha = 0.3f)
                        } else {
                            ZoyaTheme.colors.accent.copy(alpha = 0.2f)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
