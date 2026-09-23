package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ContinuousHudScanlinesOverlay
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskCenterScreen
import com.example.ui.theme.JarvisBorderGreen
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisGreenBright
import com.example.ui.theme.JarvisGreenGlow
import com.example.ui.theme.JarvisSpaceBlack
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextSecondary

data class NavItem(val title: String, val icon: ImageVector, val tag: String)

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("CHAT", Icons.Default.ChatBubble, "nav_chat"),
        NavItem("VAULT", Icons.AutoMirrored.Filled.Assignment, "nav_tasks"),
        NavItem("HISTORY", Icons.Default.History, "nav_history"),
        NavItem("SYSTEM", Icons.Default.Settings, "nav_settings")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisSpaceBlack,
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(listOf(JarvisBorderGreen, Color.Transparent))
                        )
                    )
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) JarvisGreenGlow else JarvisTextMuted
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (isSelected) JarvisGreenGlow else JarvisTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = JarvisGreenGlow,
                            unselectedIconColor = JarvisTextMuted,
                            indicatorColor = JarvisGreen.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { selectedTab = 3 }
                )
                1 -> TaskCenterScreen(viewModel = viewModel)
                2 -> HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { selectedTab = 0 }
                )
                3 -> SettingsScreen(viewModel = viewModel)
            }

            // Continuous Iron Man HUD scan-line and laser sweep overlay across main Compose screen
            ContinuousHudScanlinesOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("continuous_hud_scanlines_overlay"),
                scanColor = JarvisGreen,
                beamColor = JarvisGreenGlow,
                scanDurationMillis = 5200,
                lineSpacing = 4.dp,
                showCornerBrackets = true
            )
        }
    }
}
