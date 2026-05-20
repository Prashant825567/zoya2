package com.zoya.ai.ui.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.data.AVAILABLE_MODELS
import com.zoya.ai.data.SessionState
import com.zoya.ai.ui.components.*
import com.zoya.ai.ui.theme.*
import com.zoya.ai.viewmodel.ZoyaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomePage(viewModel: ZoyaViewModel, onRequestPermission: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val showModelPicker by viewModel.showModelPicker.collectAsState()
    val textInput by viewModel.textInput.collectAsState()
    val isMuted by viewModel.isMicMuted.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val isActive = state != SessionState.DISCONNECTED && state != SessionState.IDLE
    val isIdle = state == SessionState.IDLE

    // Animated corner colors
    val infiniteTransition = rememberInfiniteTransition(label = "corners")
    val cornerHue by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "hue"
    )

    // Corner glow colors based on state
    val cornerColor = when (state) {
        SessionState.SPEAKING -> Color(0xFFEC4899) // pink when speaking
        SessionState.LISTENING -> Color(0xFFA855F7) // purple when listening
        SessionState.CONNECTING -> Color(0xFFFACC15) // yellow connecting
        SessionState.IDLE -> Color(0xFF64748B).copy(alpha = 0.3f) // dim gray idle
        SessionState.DISCONNECTED -> Color.Transparent
    }

    val cornerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        Modifier.fillMaxSize()
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        val strokeW = 3.dp.toPx()
                        val len = 80.dp.toPx()
                        val c = cornerColor.copy(alpha = cornerAlpha)
                        val brush = Brush.linearGradient(listOf(c, c.copy(alpha = 0f)))
                        // Top-left
                        drawLine(brush, Offset(0f, 0f), Offset(len, 0f), strokeW)
                        drawLine(brush, Offset(0f, 0f), Offset(0f, len), strokeW)
                        // Top-right
                        drawLine(Brush.linearGradient(listOf(c.copy(alpha = 0f), c)), Offset(size.width - len, 0f), Offset(size.width, 0f), strokeW)
                        drawLine(brush, Offset(size.width, 0f), Offset(size.width, len), strokeW)
                        // Bottom-left
                        drawLine(brush, Offset(0f, size.height), Offset(len, size.height), strokeW)
                        drawLine(Brush.linearGradient(listOf(c.copy(alpha = 0f), c)), Offset(0f, size.height - len), Offset(0f, size.height), strokeW)
                        // Bottom-right
                        drawLine(Brush.linearGradient(listOf(c.copy(alpha = 0f), c)), Offset(size.width - len, size.height), Offset(size.width, size.height), strokeW)
                        drawLine(Brush.linearGradient(listOf(c.copy(alpha = 0f), c)), Offset(size.width, size.height - len), Offset(size.width, size.height), strokeW)
                    }
                } else Modifier
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ZoyaGradient), contentAlignment = Alignment.Center) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("ZOYA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZoyaTheme.colors.accent)
                        Text("Hi, ${userData?.name ?: "User"} • ${selectedMode.emoji} ${selectedMode.name}", fontSize = 10.sp, color = ZoyaTheme.colors.muted.copy(0.6f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(
                        when {
                            isActive -> ZoyaTheme.colors.green
                            isIdle -> Color(0xFFFACC15)
                            else -> ZoyaTheme.colors.muted
                        }
                    ))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when { isActive -> "Live"; isIdle -> "Idle"; else -> "Offline" },
                        fontSize = 10.sp, color = ZoyaTheme.colors.muted.copy(0.6f)
                    )
                }
            }

            // Main Content
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("ZOYA", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ZoyaTheme.colors.accent)
                Spacer(Modifier.height(4.dp))
                Text(
                    when (state) {
                        SessionState.DISCONNECTED -> "Hey ${userData?.name ?: ""}! Tap to wake me up 💜"
                        SessionState.CONNECTING -> "✨ Waking up..."
                        SessionState.LISTENING -> if (isMuted) "🔇 Mic is muted" else "🎧 Go ahead, I'm all ears..."
                        SessionState.SPEAKING -> "💬 Hold on, talking..."
                        SessionState.IDLE -> "😴 Say \"Zoya\" to wake me up"
                    },
                    fontSize = 13.sp, color = ZoyaTheme.colors.muted, textAlign = TextAlign.Center
                )

                // Model selector (disconnected)
                AnimatedVisibility(state == SessionState.DISCONNECTED) {
                    ModelSelectorButton(selectedModel) { viewModel.toggleModelPicker() }
                }

                // Messages
                AnimatedVisibility(isActive && messages.isNotEmpty()) {
                    val ls = rememberLazyListState()
                    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) ls.animateScrollToItem(messages.size - 1) }
                    LazyColumn(state = ls, modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(messages) { msg ->
                            Column(horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Surface(shape = RoundedCornerShape(14.dp), color = if (msg.isUser) ZoyaTheme.colors.accent else ZoyaTheme.colors.card.copy(0.8f)) {
                                    Text(msg.text, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                }
                                Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)), fontSize = 9.sp, color = ZoyaTheme.colors.muted.copy(0.4f), modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                AudioVisualizer(state)
                Spacer(Modifier.height(12.dp))

                // Buttons row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    AnimatedVisibility(isActive) {
                        IconButton(
                            onClick = { viewModel.toggleMicMute() },
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(if (isMuted) Color(0xFFEF4444).copy(0.2f) else ZoyaTheme.colors.card.copy(0.6f))
                        ) {
                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, "Mute",
                                tint = if (isMuted) Color(0xFFEF4444) else ZoyaTheme.colors.accent, modifier = Modifier.size(22.dp))
                        }
                    }
                    if (isActive) Spacer(Modifier.width(20.dp))

                    // Main power button / wake button
                    if (isIdle) {
                        // Idle: show pulsing wake button
                        WakeButton { viewModel.wakeUp(onRequestPermission) }
                    } else {
                        PowerButton(state) { viewModel.onToggleConnection(onRequestPermission) }
                    }

                    if (isActive) Spacer(Modifier.width(68.dp))
                }

                Spacer(Modifier.height(12.dp))
                StatusIndicator(state, error)
            }

            // Text input
            AnimatedVisibility(isActive, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), shape = RoundedCornerShape(20.dp), color = ZoyaTheme.colors.card.copy(0.6f)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = textInput, onValueChange = { viewModel.updateTextInput(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(ZoyaTheme.colors.accent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { viewModel.sendTextMessage() }),
                            decorationBox = { inner -> Box { if (textInput.isEmpty()) Text("Type a message...", color = ZoyaTheme.colors.muted.copy(0.4f), fontSize = 14.sp); inner() } }
                        )
                        Spacer(Modifier.width(10.dp))
                        val canSend = textInput.trim().isNotEmpty() && state != SessionState.CONNECTING
                        IconButton(
                            onClick = { viewModel.sendTextMessage() }, enabled = canSend,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).then(
                                if (canSend) Modifier.background(ZoyaGradient) else Modifier.background(Color(0x26647488))
                            )
                        ) { Icon(Icons.Default.Send, "Send", tint = if (canSend) Color.White else ZoyaTheme.colors.muted.copy(0.3f), modifier = Modifier.size(18.dp)) }
                    }
                }
            }

            if (!isActive && !isIdle) {
                Text("Powered by ${selectedModel.name}", fontSize = 10.sp, color = ZoyaTheme.colors.muted.copy(0.25f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }
        }

        if (showModelPicker) ModelPickerDialog(selectedModel, AVAILABLE_MODELS, { viewModel.selectModel(it) }, { viewModel.dismissModelPicker() })
    }
}

@Composable
fun WakeButton(onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "wake")
    val scale by inf.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse), label = "s")
    val alpha by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "a")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        // Glow
        Box(Modifier.size(130.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFA855F7).copy(alpha = alpha * 0.3f), Color.Transparent))))
        // Button
        Box(
            Modifier.size((120 * scale).dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2A2A40), Color(0xFF1E1E2E))))
                .border(2.dp, Color(0xFFA855F7).copy(alpha = alpha), CircleShape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎙️", fontSize = 28.sp)
                Text("Say \"Zoya\"", fontSize = 10.sp, color = Color(0xFFA855F7).copy(alpha = 0.7f))
            }
        }
    }
}
