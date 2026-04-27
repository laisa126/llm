package com.laiserdev.localllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            LocalLLMTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDeep) {
                    val vm: MainViewModel = viewModel()
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
}
