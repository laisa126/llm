package com.laiserdev.localllm.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.laiserdev.localllm.ui.screens.chat.ChatScreen
import com.laiserdev.localllm.ui.screens.developer.DeveloperScreen
import com.laiserdev.localllm.ui.screens.editor.EditorScreen
import com.laiserdev.localllm.ui.screens.models.ModelsScreen
import com.laiserdev.localllm.ui.screens.settings.SettingsScreen
import com.laiserdev.localllm.ui.screens.terminal.TerminalScreen
import com.laiserdev.localllm.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat      : Screen("chat",      "Chat",      Icons.Default.Chat)
    object Editor    : Screen("editor",    "Editor",    Icons.Default.Code)
    object Terminal  : Screen("terminal",  "Terminal",  Icons.Default.Terminal)
    object Models    : Screen("models",    "Models",    Icons.Default.Memory)
    object Developer : Screen("developer", "API",       Icons.Default.Api)
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings)
}

val bottomScreens = listOf(
    Screen.Chat, Screen.Editor, Screen.Terminal,
    Screen.Models, Screen.Developer, Screen.Settings
)

@Composable
fun AppNavigation(vm: MainViewModel) {
    val navController = rememberNavController()
    val currentBack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BgSurface,
                tonalElevation = 0.dp
            ) {
                bottomScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentGreen,
                            selectedTextColor = AccentGreen,
                            indicatorColor = BgElevated,
                            unselectedIconColor = TextSecond,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = BgDeep
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() + slideInHorizontally() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Chat.route)      { ChatScreen(vm) }
            composable(Screen.Editor.route)    { EditorScreen(vm) }
            composable(Screen.Terminal.route)  { TerminalScreen(vm) }
            composable(Screen.Models.route)    { ModelsScreen(vm) }
            composable(Screen.Developer.route) { DeveloperScreen(vm) }
            composable(Screen.Settings.route)  { SettingsScreen(vm) }
        }
    }
}
