package com.laiserdev.localllm.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.navigation.compose.*
import com.laiserdev.localllm.R
import com.laiserdev.localllm.ui.screens.chat.ChatScreen
import com.laiserdev.localllm.ui.screens.developer.DeveloperScreen
import com.laiserdev.localllm.ui.screens.editor.EditorScreen
import com.laiserdev.localllm.ui.screens.models.ModelsScreen
import com.laiserdev.localllm.ui.screens.preview.PreviewScreen
import com.laiserdev.localllm.ui.screens.settings.SettingsScreen
import com.laiserdev.localllm.ui.screens.terminal.TerminalScreen
import com.laiserdev.localllm.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val description: String) {
    object Chat      : Screen("chat",      "Chat",      Icons.Default.Chat,      "Talk to your local AI")
    object Editor    : Screen("editor",    "Editor",    Icons.Default.Code,      "Code editor + file tree")
    object Terminal  : Screen("terminal",  "Terminal",  Icons.Default.Terminal,  "Run shell commands")
    object Preview   : Screen("preview",   "Preview",   Icons.Default.Preview,   "Live HTML preview")
    object Models    : Screen("models",    "Models",    Icons.Default.Memory,    "Download & load models")
    object Developer : Screen("developer", "API",       Icons.Default.Api,       "Local OpenAI-compatible API")
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings,  "App configuration")
}

val allScreens = listOf(
    Screen.Chat, Screen.Editor, Screen.Terminal,
    Screen.Preview, Screen.Models, Screen.Developer, Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(vm: MainViewModel) {
    val navController = rememberNavController()
    val currentBack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBack?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentScreen = allScreens.firstOrNull { it.route == currentRoute } ?: Screen.Chat

    fun navigate(screen: Screen) {
        scope.launch { drawerState.close() }
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentScreen = currentScreen,
                onNavigate = ::navigate,
                onClose = { scope.launch { drawerState.close() } },
                vm = vm
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = BgDeep
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                modifier = Modifier.padding(padding).fillMaxSize().imePadding(),
                enterTransition = { fadeIn(tween(200)) + slideInHorizontally { it / 20 } },
                exitTransition = { fadeOut(tween(150)) }
            ) {
                composable(Screen.Chat.route)      { ChatScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable(Screen.Editor.route)    { EditorScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable(Screen.Terminal.route)  { TerminalScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable(Screen.Preview.route)   { PreviewScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable(Screen.Models.route)    { ModelsScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable(Screen.Developer.route) { DeveloperScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable(Screen.Settings.route)  { SettingsScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) }
            }
        }
    }
}

@Composable
fun AppDrawer(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onClose: () -> Unit,
    vm: MainViewModel
) {
    val activeProject by vm.activeProject.collectAsState()
    val settings by vm.settings.collectAsState()
    val models by vm.models.collectAsState()
    val loadedModel = models.firstOrNull { it.id == settings.activeModelId && it.status.name == "LOADED" }

    ModalDrawerSheet(
        modifier = Modifier.width(290.dp),
        drawerContainerColor = Color(0xFF07090E),
        drawerContentColor = TextPrimary,
        windowInsets = WindowInsets.systemBars
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0A1525), Color(0xFF07090E)))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_app_logo),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Local LLM Agent", color = TextPrimary, fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                                Text("On-device AI", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = onClose, Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = TextMuted)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Model status pill
                    Row(
                        Modifier.fillMaxWidth()
                            .background(
                                if (loadedModel != null) Color(0xFF00150E) else Color(0xFF0F1419),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                0.5.dp,
                                if (loadedModel != null) AccentGreen.copy(0.35f) else BgBorderBright,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(7.dp)
                                .background(
                                    if (loadedModel != null) AccentGreen else TextMuted,
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            loadedModel?.name ?: "No model loaded",
                            color = if (loadedModel != null) AccentGreen else TextMuted,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (loadedModel == null)
                            Text("→ Models", color = TextMuted, fontSize = 10.sp)
                    }

                    // Active project
                    if (activeProject != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Color(0xFF0A1020), RoundedCornerShape(8.dp))
                                .border(0.5.dp, AccentBlue.copy(0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, null,
                                Modifier.size(13.dp), tint = AccentBlue)
                            Spacer(Modifier.width(6.dp))
                            Text(activeProject?.name ?: "", color = AccentBlue, fontSize = 11.sp,
                                modifier = Modifier.weight(1f))
                            Text("active", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))

            // ── Nav items ─────────────────────────────────────────────────────
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DrawerSectionLabel("WORKSPACE")
                DrawerNavItem(Screen.Chat, currentScreen, onNavigate)
                DrawerNavItem(Screen.Editor, currentScreen, onNavigate)
                DrawerNavItem(Screen.Terminal, currentScreen, onNavigate)
                DrawerNavItem(Screen.Preview, currentScreen, onNavigate)
                Spacer(Modifier.height(6.dp))
                DrawerSectionLabel("SYSTEM")
                DrawerNavItem(Screen.Models, currentScreen, onNavigate)
                DrawerNavItem(Screen.Developer, currentScreen, onNavigate)
                DrawerNavItem(Screen.Settings, currentScreen, onNavigate)
            }

            // ── Footer ────────────────────────────────────────────────────────
            HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(11.dp), tint = TextMuted)
                Spacer(Modifier.width(6.dp))
                Text("100% on-device · no data leaves phone",
                    color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerNavItem(screen: Screen, currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    val selected = screen.route == currentScreen.route
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (selected) Brush.horizontalGradient(
                    listOf(AccentCyan.copy(0.1f), AccentPurple.copy(0.05f))
                )
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                RoundedCornerShape(10.dp)
            )
            .border(
                if (selected) BorderStroke(0.5.dp,
                    Brush.horizontalGradient(listOf(AccentCyan.copy(0.4f), AccentPurple.copy(0.3f))))
                else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(10.dp)
            )
            .clickable { onNavigate(screen) }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            screen.icon, null, Modifier.size(18.dp),
            tint = if (selected) AccentCyan else TextSecond
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                screen.label,
                color = if (selected) TextPrimary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(screen.description, color = TextMuted, fontSize = 11.sp)
        }
        if (selected) {
            Box(Modifier.size(5.dp)
                .background(
                    Brush.radialGradient(listOf(AccentCyan, AccentPurple)),
                    CircleShape
                )
            )
        }
    }
}

// ── Shared top bar ────────────────────────────────────────────────────────────
@Composable
fun AppTopBar(
    title: String,
    onOpenDrawer: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .background(Color(0xFF07090E))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer, Modifier.size(40.dp)) {
                Icon(Icons.Default.Menu, "Open menu", Modifier.size(22.dp), tint = TextSecond)
            }
            Text(
                title, color = TextPrimary, fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            actions()
        }
        HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
    }
}
