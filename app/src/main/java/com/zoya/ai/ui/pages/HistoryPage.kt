package com.zoya.ai.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.data.MessageSource
import com.zoya.ai.ui.theme.ZoyaTheme
import com.zoya.ai.viewmodel.ZoyaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryPage(viewModel: ZoyaViewModel) {
    val history by viewModel.history.collectAsState()

    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Chat History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Stored for 24 hours • voice + text", fontSize = 11.sp, color = ZoyaTheme.colors.muted)
            }
            IconButton(onClick = { viewModel.clearHistory() }) {
                Icon(Icons.Default.DeleteSweep, "Clear", tint = Color(0xFFEF4444))
            }
        }
        Spacer(Modifier.height(12.dp))

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No chat history yet", fontSize = 14.sp, color = ZoyaTheme.colors.muted)
                    Text("Voice and text conversations will appear here", fontSize = 11.sp, color = ZoyaTheme.colors.muted.copy(0.5f))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(history) { msg ->
                    val timeText = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                    val sourceLabel = if (msg.source == MessageSource.VOICE) "Voice" else "Text"
                    val senderLabel = if (msg.isUser) "You" else "Zoya"
                    val sourceEmoji = if (msg.source == MessageSource.VOICE) "🎙️" else "⌨️"

                    Column(
                        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (msg.isUser) ZoyaTheme.colors.accent.copy(0.85f) else ZoyaTheme.colors.card,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(senderLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (msg.source == MessageSource.VOICE) Color(0xFFEC4899).copy(alpha = 0.18f) else Color(0xFF64748B).copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            "$sourceEmoji $sourceLabel",
                                            fontSize = 9.sp,
                                            color = if (msg.source == MessageSource.VOICE) Color(0xFFF9A8D4) else Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(msg.text, fontSize = 13.sp, color = Color.White, lineHeight = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "$timeText • ${msg.modelName}",
                            fontSize = 9.sp,
                            color = ZoyaTheme.colors.muted.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
