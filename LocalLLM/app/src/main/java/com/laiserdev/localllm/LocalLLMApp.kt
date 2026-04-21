package com.laiserdev.localllm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.laiserdev.localllm.data.repository.ApiKeyRepository
import com.laiserdev.localllm.data.repository.LLMRepository
import com.laiserdev.localllm.data.repository.ProjectRepository
import com.laiserdev.localllm.util.PackageManager
import com.laiserdev.localllm.util.SettingsManager
import com.laiserdev.localllm.util.SkillsManager
import com.laiserdev.localllm.util.TerminalExecutor
import com.laiserdev.localllm.util.ToolEngine

class LocalLLMApp : Application() {

    // Manual DI - singletons
    val llmRepository by lazy { LLMRepository(this) }
    val projectRepository by lazy { ProjectRepository(this) }
    val apiKeyRepository by lazy { ApiKeyRepository() }
    val settingsManager by lazy { SettingsManager(this) }
    val terminalExecutor by lazy { TerminalExecutor(this) }
    val packageManager by lazy { PackageManager(this) }
    val skillsManager by lazy { SkillsManager(this) }
    val toolEngine by lazy { ToolEngine(this, llmRepository, terminalExecutor, packageManager, projectRepository) }

    companion object {
        const val CHANNEL_SERVER   = "llm_server"
        const val CHANNEL_DOWNLOAD = "llm_download"
        lateinit var instance: LocalLLMApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_SERVER, "API Server", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "LocalLLM API server on port 8080" })
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_DOWNLOAD, "Model Downloads", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "AI model download progress" })
        }
    }
}
