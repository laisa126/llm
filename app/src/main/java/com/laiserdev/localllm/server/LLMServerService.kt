package com.laiserdev.localllm.server

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.laiserdev.localllm.BuildConfig
import com.laiserdev.localllm.LocalLLMApp
import com.laiserdev.localllm.MainActivity
import com.laiserdev.localllm.data.model.ApiError
import com.laiserdev.localllm.data.model.ApiRequest
import com.laiserdev.localllm.data.model.ApiResponse
import com.laiserdev.localllm.data.repository.ApiKeyRepository
import com.laiserdev.localllm.data.repository.LLMRepository
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

class LLMServerService : Service() {
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var llmRepo: LLMRepository
    private lateinit var apiKeyRepo: ApiKeyRepository

    companion object {
        var isRunning = false
        const val PORT = BuildConfig.API_PORT
    }

    override fun onCreate() {
        super.onCreate()
        // Use the app-level singleton so the same loaded model is shared
        val app = applicationContext as LocalLLMApp
        llmRepo = app.llmRepository
        apiKeyRepo = app.apiKeyRepository
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1001, buildNotification()); startServer(); isRunning = true; return START_STICKY
    }
    override fun onDestroy() {
        server?.stop(500, 1000, TimeUnit.MILLISECONDS); isRunning = false; super.onDestroy()
    }

    private fun startServer() {
        scope.launch {
            try {
                server = embeddedServer(Netty, port = PORT) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                    install(CORS) {
                        anyHost(); allowHeader(HttpHeaders.ContentType); allowHeader("X-API-Key")
                        allowMethod(HttpMethod.Post); allowMethod(HttpMethod.Get); allowMethod(HttpMethod.Options)
                    }
                    routing {
                        get("/") { call.respond(mapOf("service" to "LocalLLM API", "model" to (llmRepo.currentModel() ?: "none"))) }
                        get("/health") { call.respond(mapOf("status" to "ok", "modelLoaded" to llmRepo.isLoaded())) }
                        route("/v1") {
                            intercept(ApplicationCallPipeline.Plugins) {
                                val key = call.request.headers["X-API-Key"] ?: call.request.queryParameters["api_key"]
                                if (key.isNullOrBlank()) { call.respond(HttpStatusCode.Unauthorized, ApiError("Missing X-API-Key", 401)); finish(); return@intercept }
                                if (apiKeyRepo.validateKey(key).isFailure) { call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid API key", 401)); finish(); return@intercept }
                            }
                            post("/chat") {
                                if (!llmRepo.isLoaded()) { call.respond(HttpStatusCode.ServiceUnavailable, ApiError("No model loaded", 503)); return@post }
                                val req = call.receive<ApiRequest>(); val t = System.currentTimeMillis()
                                llmRepo.generate(req.prompt, req.system ?: "", req.maxTokens).fold(
                                    onSuccess = { call.respond(ApiResponse(UUID.randomUUID().toString(), it, llmRepo.currentModel() ?: "unknown", latencyMs = System.currentTimeMillis() - t)) },
                                    onFailure = { call.respond(HttpStatusCode.InternalServerError, ApiError(it.message ?: "Error", 500)) }
                                )
                            }
                            post("/code") {
                                if (!llmRepo.isLoaded()) { call.respond(HttpStatusCode.ServiceUnavailable, ApiError("No model loaded", 503)); return@post }
                                val req = call.receive<ApiRequest>(); val t = System.currentTimeMillis()
                                llmRepo.generateCode(req.prompt).fold(
                                    onSuccess = { call.respond(ApiResponse(UUID.randomUUID().toString(), it, llmRepo.currentModel() ?: "unknown", latencyMs = System.currentTimeMillis() - t)) },
                                    onFailure = { call.respond(HttpStatusCode.InternalServerError, ApiError(it.message ?: "Error", 500)) }
                                )
                            }
                            get("/models") { call.respond(mapOf("loaded" to llmRepo.currentModel(), "capabilities" to listOf("chat", "code"))) }
                        }
                    }
                }
                server!!.start(wait = true)
            } catch (e: Exception) { isRunning = false }
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, LocalLLMApp.CHANNEL_SERVER)
        .setContentTitle("LocalLLM API Running").setContentText("localhost:$PORT")
        .setSmallIcon(android.R.drawable.ic_menu_upload).setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .build()
}
