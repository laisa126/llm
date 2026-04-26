package com.laiserdev.localllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laiserdev.localllm.ui.MainViewModel
import com.laiserdev.localllm.ui.AppNavigation
import com.laiserdev.localllm.ui.theme.BgDeep
import com.laiserdev.localllm.ui.theme.LocalLLMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let Compose handle insets — required when windowSoftInputMode=adjustNothing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            LocalLLMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    color = BgDeep
                ) {
                    val vm: MainViewModel = viewModel()
                    AppNavigation(vm = vm)
                }
            }
        }
    }
}
