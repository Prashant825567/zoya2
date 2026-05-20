package com.zoya.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.data.SessionState
import com.zoya.ai.ui.theme.ZoyaTheme

@Composable
fun StatusIndicator(
    state: SessionState,
    error: String?
) {
    AnimatedContent(
        targetState = error to state,
        transitionSpec = {
            fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
        },
        label = "status"
    ) { (currentError, currentState) ->
        if (currentError != null) {
            // Error indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ZoyaTheme.colors.card.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ZoyaTheme.colors.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentError,
                        fontSize = 12.sp,
                        color = ZoyaTheme.colors.error,
                        maxLines = 1
                    )
                }
            }
        } else {
            // Normal status
            val (label, dotColor, textColor) = when (currentState) {
                SessionState.DISCONNECTED -> Triple(
                    "Tap to activate",
                    ZoyaTheme.colors.muted,
                    ZoyaTheme.colors.muted
                )
                SessionState.CONNECTING -> Triple(
                    "Connecting...",
                    Color(0xFFFACC15),
                    Color(0xFFFACC15)
                )
                SessionState.LISTENING -> Triple(
                    "Listening...",
                    ZoyaTheme.colors.accent,
                    ZoyaTheme.colors.accent
                )
                SessionState.SPEAKING -> Triple(
                    "Zoya is speaking",
                    ZoyaTheme.colors.accent2,
                    ZoyaTheme.colors.accent2
                )
                SessionState.IDLE -> Triple(
                    "Say \"Zoya\" to wake",
                    Color(0xFFFACC15),
                    Color(0xFFFACC15)
                )
            }
            
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ZoyaTheme.colors.card.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
