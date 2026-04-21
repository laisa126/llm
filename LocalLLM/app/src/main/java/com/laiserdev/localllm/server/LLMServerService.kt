package com.laiserdev.localllm.server

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import io.ktor.server.auth.*
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

class LLMServerService : Service() {

    private var server: EmbeddedServer<*, *>? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var llmRepo: LLMRepository
    private lateinit var apiKeyRepo: ApiKeyRepository

    companion object {
        var isRunning = false
        const val PORT = BuildConfig.API_PORT
    }

    override fun onCreate() {
        super.onCreate()
        llmRepo = LLMRepository(applicationContext)
        apiKeyRepo = ApiKeyRepository()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1001, buildNotification())
        startServer()
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop(500, 1000)
        isRunning = false
        super.onDestroy()
    }

    private fun startServer() {
        scope.launch {
            try {
                server = embeddedServer(Netty, port = PORT) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; prettyPrint = false })
                    }
                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.ContentType)
                        allowHeader("X-API-Key")
                        allowMethod(HttpMethod.Post)
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Options)
                    }

                    routing {
                        // ── Public health check ──────────────────────────────
                        get("/") {
                            call.respond(mapOf(
                                "service" to "LocalLLM API",
                                "version" to "1.0.0",
                                "model" to (llmRepo.currentModel() ?: "none"),
                                "status" to "running"
                            ))
                        }

                        get("/health") {
                            call.respond(mapOf(
                                "status" to "ok",
                                "modelLoaded" to llmRepo.isLoaded()
                            ))
                        }

                        // ── Protected routes ─────────────────────────────────
                        route("/v1") {
                            // Auth middleware
                            intercept(ApplicationCallPipeline.Plugins) {
                                val apiKey = call.request.headers["X-API-Key"]
                                    ?: call.request.queryParameters["api_key"]
                                if (apiKey.isNullOrBlank()) {
                                    call.respond(
                                        HttpStatusCode.Unauthorized,
                                        ApiError("Missing API key. Pass via X-API-Key header.", 401)
                                    )
                                    finish()
                                    return@intercept
                                }
                                val result = apiKeyRepo.validateKey(apiKey)
                                if (result.isFailure) {
                                    call.respond(
                                        HttpStatusCode.Unauthorized,
                                        ApiError("Invalid or inactive API key", 401)
                                    )
                                    finish()
                                    return@intercept
                                }
                            }

                            // ── Chat completion ──────────────────────────────
                            post("/chat") {
                                if (!llmRepo.isLoaded()) {
                                    call.respond(
                                        HttpStatusCode.ServiceUnavailable,
                                        ApiError("No model loaded on device", 503)
                                    )
                                    return@post
                                }
                                val req = call.receive<ApiRequest>()
                                val start = System.currentTimeMillis()
                                val result = llmRepo.generate(
                                    prompt = req.prompt,
                                    systemPrompt = req.system ?: "",
                                    maxTokens = req.maxTokens
                                )
                                result.fold(
                                    onSuccess = { content ->
                                        call.respond(ApiResponse(
                                            id = UUID.randomUUID().toString(),
                                            content = content,
                                            model = llmRepo.currentModel() ?: "unknown",
                                            latencyMs = System.currentTimeMillis() - start
                                        ))
                                    },
                                    onFailure = { e ->
                                        call.respond(
                                            HttpStatusCode.InternalServerError,
                                            ApiError(e.message ?: "Inference error", 500)
                                        )
                                    }
                                )
                            }

                            // ── Code generation ──────────────────────────────
                            post("/code") {
                                if (!llmRepo.isLoaded()) {
                                    call.respond(
                                        HttpStatusCode.ServiceUnavailable,
                                        ApiError("No model loaded on device", 503)
                                    )
                                    return@post
                                }
                                val req = call.receive<ApiRequest>()
                                val start = System.currentTimeMillis()
                                val result = llmRepo.generateCode(
                                    instruction = req.prompt,
                                    language = "auto"
                                )
                                result.fold(
                                    onSuccess = { content ->
                                        call.respond(ApiResponse(
                                            id = UUID.randomUUID().toString(),
                                            content = content,
                                            model = llmRepo.currentModel() ?: "unknown",
                                            latencyMs = System.currentTimeMillis() - start
                                        ))
                                    },
                                    onFailure = { e ->
                                        call.respond(
                                            HttpStatusCode.InternalServerError,
                                            ApiError(e.message ?: "Code generation error", 500)
                                        )
                                    }
                                )
                            }

                            // ── Models info ──────────────────────────────────
                            get("/models") {
                                call.respond(mapOf(
                                    "loaded" to llmRepo.currentModel(),
                                    "capabilities" to listOf("chat", "code")
                                ))
                            }
                        }
                    }
                }
                server!!.start(wait = true)
            } catch (e: Exception) {
                isRunning = false
            }
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, LocalLLMApp.CHANNEL_SERVER)
            .setContentTitle("LocalLLM API Running")
            .setContentText("http://localhost:$PORT  •  Tap to open app")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
}
