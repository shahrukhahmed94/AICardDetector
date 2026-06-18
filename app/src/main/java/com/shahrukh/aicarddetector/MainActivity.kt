package com.shahrukh.aicarddetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahrukh.aicarddetector.presentation.AppScreen
import com.shahrukh.aicarddetector.presentation.ScanViewModel
import com.shahrukh.aicarddetector.presentation.screens.*
import com.shahrukh.aicarddetector.ui.theme.AICardDetectorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AICardDetectorTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E1A))) {
                    // Page Routing
                    when (currentScreen) {
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                        AppScreen.SCAN -> {
                            ScanScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                        AppScreen.RESULTS -> {
                            ResultsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }

                    // Floating Bottom Nav Bar shown on Dashboard, History, and Settings
                    if (currentScreen == AppScreen.DASHBOARD || 
                        currentScreen == AppScreen.HISTORY || 
                        currentScreen == AppScreen.SETTINGS
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            FloatingBottomBar(
                                currentScreen = currentScreen,
                                onTabSelected = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingBottomBar(
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit
) {
    val items = listOf(
        NavigationItem("Home", Icons.Default.Home, AppScreen.DASHBOARD),
        NavigationItem("Logs", Icons.Default.List, AppScreen.HISTORY),
        NavigationItem("Setup", Icons.Default.Settings, AppScreen.SETTINGS)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6161F30)) // Slate blue dark with alpha
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            val activeColor = Color(0xFF00FFCC) // Neon cyan
            val inactiveColor = Color.White.copy(alpha = 0.4f)

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(item.screen) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) activeColor else inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) activeColor else inactiveColor
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val screen: AppScreen
)
