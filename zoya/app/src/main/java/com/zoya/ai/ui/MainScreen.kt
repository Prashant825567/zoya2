package com.zoya.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.ui.pages.*
import com.zoya.ai.ui.theme.ZoyaTheme
import com.zoya.ai.viewmodel.ZoyaViewModel

@Composable
fun MainScreen(viewModel: ZoyaViewModel, onRequestPermission: () -> Unit) {
    val userData by viewModel.userData.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()

    if (authLoading) {
        Box(Modifier.fillMaxSize().background(ZoyaTheme.colors.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ZoyaTheme.colors.accent)
        }
        return
    }

    if (userData == null) {
        AccessKeyScreen(viewModel)
    } else {
        AppWithTabs(viewModel, onRequestPermission)
    }
}

@Composable
fun AppWithTabs(viewModel: ZoyaViewModel, onRequestPermission: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "History" to Icons.Default.History,
        "Settings" to Icons.Default.Settings
    )

    Scaffold(
        containerColor = ZoyaTheme.colors.background,
        bottomBar = {
            NavigationBar(
                containerColor = ZoyaTheme.colors.card,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index == 1) viewModel.loadHistory()
                        },
                        icon = { Icon(icon, label, tint = if (selectedTab == index) ZoyaTheme.colors.accent else ZoyaTheme.colors.muted) },
                        label = { Text(label, fontSize = 11.sp, color = if (selectedTab == index) ZoyaTheme.colors.accent else ZoyaTheme.colors.muted) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = ZoyaTheme.colors.accent.copy(alpha = 0.12f))
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomePage(viewModel, onRequestPermission)
                1 -> HistoryPage(viewModel)
                2 -> SettingsPage(viewModel)
            }
        }
    }
}
