package com.zoya.ai.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.data.PERSONALITY_MODES
import com.zoya.ai.ui.theme.ZoyaGradient
import com.zoya.ai.ui.theme.ZoyaTheme
import com.zoya.ai.viewmodel.ZoyaViewModel

@Composable
fun SettingsPage(viewModel: ZoyaViewModel) {
    val userData by viewModel.userData.collectAsState()
    val user = userData ?: return
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(20.dp))

        // Profile
        Surface(shape = RoundedCornerShape(18.dp), color = ZoyaTheme.colors.card,
            modifier = Modifier.fillMaxWidth().border(1.dp, ZoyaTheme.colors.accent.copy(0.1f), RoundedCornerShape(18.dp))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(ZoyaGradient), contentAlignment = Alignment.Center) {
                    Text(user.name.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Name cannot be changed", fontSize = 10.sp, color = ZoyaTheme.colors.muted.copy(0.5f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Personality Modes
        Text("Zoya's Personality", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text("Choose how Zoya talks to you", fontSize = 11.sp, color = ZoyaTheme.colors.muted)
        Spacer(Modifier.height(12.dp))

        PERSONALITY_MODES.forEach { mode ->
            val isCurrent = mode.id == selectedMode.id

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCurrent) ZoyaTheme.colors.accent.copy(0.1f) else ZoyaTheme.colors.card,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .border(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) ZoyaTheme.colors.accent.copy(0.4f) else ZoyaTheme.colors.border.copy(0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(remember { MutableInteractionSource() }, null) {
                        viewModel.setPersonalityMode(mode)
                    }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Emoji avatar
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) ZoyaTheme.colors.accent.copy(0.15f) else ZoyaTheme.colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.emoji, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mode.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Text(mode.description, fontSize = 11.sp, color = ZoyaTheme.colors.muted.copy(0.7f), lineHeight = 15.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (isCurrent) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(ZoyaTheme.colors.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .border(1.5.dp, ZoyaTheme.colors.muted.copy(0.25f), CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Info items
        SettingItem(Icons.Default.Key, "Access Key", user.accessKey)
        SettingItem(Icons.Default.PhoneAndroid, "Device Bound", "This device only")
        SettingItem(Icons.Default.SmartToy, "Current Model", selectedModel.name)
        SettingItem(Icons.Default.History, "Chat History", "Stored for 24 hours")

        Spacer(Modifier.height(24.dp))

        // Logout
        Button(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(0.15f))
        ) { Text("Logout", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold) }

        Spacer(Modifier.height(20.dp))
        Text("Zoya AI v1.0", fontSize = 10.sp, color = ZoyaTheme.colors.muted.copy(0.3f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SettingItem(icon: ImageVector, label: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = ZoyaTheme.colors.card, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, tint = ZoyaTheme.colors.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column { Text(label, fontSize = 12.sp, color = ZoyaTheme.colors.muted); Text(value, fontSize = 13.sp, color = Color.White) }
        }
    }
}
