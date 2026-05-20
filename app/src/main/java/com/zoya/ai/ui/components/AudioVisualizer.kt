package com.zoya.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zoya.ai.data.SessionState
import kotlin.math.sin

@Composable
fun AudioVisualizer(state: SessionState) {
    val barCount = 32
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    if (state == SessionState.DISCONNECTED || state == SessionState.IDLE) {
        Spacer(modifier = Modifier.height(64.dp))
        return
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val position = i.toFloat() / barCount
            
            // Calculate dynamic height based on state
            val height = when (state) {
                SessionState.SPEAKING -> {
                    val wave1 = sin((time * 0.8f + i * 0.4f).toDouble()).toFloat() * 0.5f + 0.5f
                    val wave2 = sin((time * 1.2f + i * 0.6f).toDouble()).toFloat() * 0.3f + 0.5f
                    val wave3 = sin((time * 0.5f + i * 0.2f).toDouble()).toFloat() * 0.2f + 0.5f
                    val combined = (wave1 + wave2 + wave3) / 3f
                    6f + combined * 42f
                }
                SessionState.LISTENING -> {
                    val wave = sin((time * 0.2f + i * 0.3f).toDouble()).toFloat() * 0.5f + 0.5f
                    4f + wave * 16f
                }
                SessionState.CONNECTING -> {
                    val wave = sin((time * 0.3f + i * 0.5f).toDouble()).toFloat() * 0.5f + 0.5f
                    3f + wave * 8f
                }
                else -> 4f
            }
            
            // Gradient color from purple to pink
            val r = (168 + (236 - 168) * position).toInt()
            val g = (85 + (72 - 85) * position).toInt()
            val b = (247 + (153 - 247) * position).toInt()
            
            val color = when (state) {
                SessionState.SPEAKING -> Color(r, g, b)
                SessionState.CONNECTING -> Color(0xFF4A4A6A)
                else -> Color(r, g, b, alpha = (0.6f * 255).toInt())
            }
            
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
