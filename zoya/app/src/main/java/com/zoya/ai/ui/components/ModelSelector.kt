package com.zoya.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zoya.ai.data.GeminiModel
import com.zoya.ai.ui.theme.ZoyaGradient
import com.zoya.ai.ui.theme.ZoyaTheme

@Composable
fun ModelSelectorButton(
    selectedModel: GeminiModel,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ZoyaTheme.colors.card.copy(alpha = 0.6f),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .border(
                width = 1.dp,
                color = ZoyaTheme.colors.accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "✨", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedModel.name,
                fontSize = 12.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            BadgeChip(
                text = selectedModel.badge,
                isRecommended = selectedModel.badge == "Recommended"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "▼",
                fontSize = 10.sp,
                color = ZoyaTheme.colors.muted
            )
        }
    }
}

@Composable
fun BadgeChip(text: String, isRecommended: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isRecommended) {
                    Modifier.background(ZoyaGradient)
                } else {
                    Modifier.background(Color(0x4D64748B))
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = if (isRecommended) Color.White else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun ModelPickerDialog(
    selectedModel: GeminiModel,
    models: List<GeminiModel>,
    onSelect: (GeminiModel) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZoyaTheme.colors.card,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = ZoyaTheme.colors.accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Text(
                    text = "Select Model",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Choose which Gemini model to use",
                    fontSize = 10.sp,
                    color = ZoyaTheme.colors.muted.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Options
                models.forEach { model ->
                    val isSelected = model.id == selectedModel.id
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            ZoyaTheme.colors.accent.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(model) }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(ZoyaGradient)
                                        } else {
                                            Modifier.background(Color(0x26647488))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✨", fontSize = 16.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = model.id.split("/").last(),
                                    fontSize = 9.sp,
                                    color = ZoyaTheme.colors.muted.copy(alpha = 0.5f)
                                )
                            }
                            
                            // Badge
                            BadgeChip(
                                text = model.badge,
                                isRecommended = model.badge == "Recommended"
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Checkbox
                            if (isSelected) {
                                Text(text = "✓", color = ZoyaTheme.colors.accent, fontSize = 16.sp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .border(
                                            width = 1.dp,
                                            color = ZoyaTheme.colors.muted.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
