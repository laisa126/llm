package com.laiserdev.localllm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laiserdev.localllm.ui.AppNavigation
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.screens.bootstrap.BootstrapPhase
import com.laiserdev.localllm.ui.screens.bootstrap.BootstrapScreen
import com.laiserdev.localllm.ui.screens.onboarding.OnboardingScreen
import com.laiserdev.localllm.ui.theme.BgDeep
import com.laiserdev.localllm.ui.theme.LocalLLMTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results handled silently; image picker degrades gracefully if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        requestRuntimePermissions()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle(
                com.laiserdev.localllm.data.model.AppSettings()
            )
            LocalLLMTheme(
                fontFamily = settings.fontFamily,
                fontSize = settings.fontSize
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDeep) {
                    val bootstrapDone     by vm.bootstrapDone.collectAsStateWithLifecycle(false)
                    val bootstrapPhase    by vm.bootstrapPhase.collectAsStateWithLifecycle(BootstrapPhase.CHECKING)
                    val bootstrapProgress by vm.bootstrapProgress.collectAsStateWithLifecycle(0f)
                    val onboardingDone    by vm.onboardingDone.collectAsStateWithLifecycle(false)

                    when {
                        !bootstrapDone  -> BootstrapScreen(progress = bootstrapProgress, phase = bootstrapPhase)
                        !onboardingDone -> OnboardingScreen(onDone = { vm.completeOnboarding() })
                        else            -> AppNavigation(vm = vm)
                    }
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = buildList {
            // Image picker — READ_MEDIA_IMAGES on API 33+, READ_EXTERNAL_STORAGE below
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                if (ContextCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            // Notification permission — required on API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}
