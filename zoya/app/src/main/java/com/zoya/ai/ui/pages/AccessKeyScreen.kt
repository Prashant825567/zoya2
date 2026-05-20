package com.zoya.ai.ui.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.ui.theme.ZoyaGradient
import com.zoya.ai.ui.theme.ZoyaTheme
import com.zoya.ai.viewmodel.ZoyaViewModel

@Composable
fun AccessKeyScreen(viewModel: ZoyaViewModel) {
    val authError by viewModel.authError.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    var keyInput by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().background(ZoyaTheme.colors.background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(ZoyaGradient),
                contentAlignment = Alignment.Center
            ) { Text("🎙️", fontSize = 32.sp) }

            Spacer(Modifier.height(16.dp))
            Text("ZOYA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ZoyaTheme.colors.accent)
            Text("AI Voice Assistant", fontSize = 12.sp, color = ZoyaTheme.colors.muted)

            Spacer(Modifier.height(32.dp))

            // Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ZoyaTheme.colors.card,
                modifier = Modifier.fillMaxWidth().border(1.dp, ZoyaTheme.colors.accent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Enter Access Key", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Enter your access key to get started", fontSize = 11.sp, color = ZoyaTheme.colors.muted, modifier = Modifier.padding(top = 4.dp))

                    Spacer(Modifier.height(20.dp))

                    BasicTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(ZoyaTheme.colors.background).padding(14.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(ZoyaTheme.colors.accent),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box {
                                if (keyInput.isEmpty()) Text("XXXX-XXXX-XXXX", color = ZoyaTheme.colors.muted.copy(0.4f), fontSize = 14.sp)
                                inner()
                            }
                        }
                    )

                    if (authError != null) {
                        Text(authError!!, color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.submitAccessKey(keyInput) },
                        enabled = keyInput.trim().length >= 4 && !authLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZoyaTheme.colors.accent)
                    ) {
                        if (authLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Activate Zoya", fontWeight = FontWeight.SemiBold)
                    }

                    Text("One key per device only", fontSize = 10.sp, color = ZoyaTheme.colors.muted.copy(0.5f), modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}
