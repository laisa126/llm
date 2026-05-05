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
import kotlinx.coroutines.flow.first
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

    companion object {
        var isRunning = false
        const val PORT = BuildConfig.API_PORT
        /** Pass as Intent extra to expose the server on the LAN (0.0.0.0).
         *  Default (false) = loopback only (127.0.0.1) — safe for production. */
        const val EXTRA_LAN_MODE = "lan_mode"
    }

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as LocalLLMApp
        llmRepo = app.llmRepository
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lanMode = intent?.getBooleanExtra(EXTRA_LAN_MODE, false) ?: false
        startForeground(1001, buildNotification(lanMode))
        startServer(lanMode)
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop(500, 1000, TimeUnit.MILLISECONDS)
        isRunning = false
        super.onDestroy()
    }

    private fun startServer(lanMode: Boolean = false) {
        val host = if (lanMode) "0.0.0.0" else "127.0.0.1"
        scope.launch {
            try {
                val apiToken = (applicationContext as com.laiserdev.localllm.LocalLLMApp)
                    .settingsManager.settings.first().apiToken
                server = embeddedServer(Netty, port = PORT, host = host) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.ContentType)
                        allowMethod(HttpMethod.Post)
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Options)
                    }
                    routing {
                        // Health and root: always open (loopback-only by default anyway)
                        get("/") {
                            call.respond(mapOf(
                                "service" to "LocalLLM API",
                                "model" to (llmRepo.currentModel() ?: "none"),
                                "version" to BuildConfig.VERSION_NAME
                            ))
                        }
                        get("/health") {
                            call.respond(mapOf(
                                "status" to "ok",
                                "modelLoaded" to llmRepo.isLoaded()
                            ))
                        }
                        route("/v1") {
                            post("/chat") {
                                if (lanMode && !isAuthorized(call, apiToken)) return@post
                                if (!llmRepo.isLoaded()) {
                                    call.respond(HttpStatusCode.ServiceUnavailable, ApiError("No model loaded", 503))
                                    return@post
                                }
                                val req = call.receive<ApiRequest>()
                                val t = System.currentTimeMillis()
                                llmRepo.generate(req.prompt, req.system ?: "", req.maxTokens).fold(
                                    onSuccess = {
                                        call.respond(ApiResponse(
                                            id = UUID.randomUUID().toString(),
                                            content = it,
                                            model = llmRepo.currentModel() ?: "unknown",
                                            latencyMs = System.currentTimeMillis() - t
                                        ))
                                    },
                                    onFailure = {
                                        call.respond(HttpStatusCode.InternalServerError,
                                            ApiError(it.message ?: "Error", 500))
                                    }
                                )
                            }
                            post("/code") {
                                if (lanMode && !isAuthorized(call, apiToken)) return@post
                                if (!llmRepo.isLoaded()) {
                                    call.respond(HttpStatusCode.ServiceUnavailable, ApiError("No model loaded", 503))
                                    return@post
                                }
                                val req = call.receive<ApiRequest>()
                                val t = System.currentTimeMillis()
                                llmRepo.generateCode(req.prompt).fold(
                                    onSuccess = {
                                        call.respond(ApiResponse(
                                            id = UUID.randomUUID().toString(),
                                            content = it,
                                            model = llmRepo.currentModel() ?: "unknown",
                                            latencyMs = System.currentTimeMillis() - t
                                        ))
                                    },
                                    onFailure = {
                                        call.respond(HttpStatusCode.InternalServerError,
                                            ApiError(it.message ?: "Error", 500))
                                    }
                                )
                            }
                            get("/models") {
                                if (lanMode && !isAuthorized(call, apiToken)) return@get
                                call.respond(mapOf(
                                    "loaded" to llmRepo.currentModel(),
                                    "capabilities" to listOf("chat", "code")
                                ))
                            }
                        }
                    }
                }
                server?.start(wait = true)
            } catch (e: Exception) {
                isRunning = false
            }
        }
    }

    /** Returns true if request carries the correct Bearer token, otherwise responds 401 and returns false. */
    private suspend fun isAuthorized(call: io.ktor.server.application.ApplicationCall, token: String): Boolean {
        val bearer = call.request.headers[io.ktor.http.HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")?.trim()
        return if (bearer == token) {
            true
        } else {
            call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid or missing Bearer token", 401))
            false
        }
    }

    private fun buildNotification(lanMode: Boolean = false): android.app.Notification {
        val address = if (lanMode) "0.0.0.0:$PORT (LAN)" else "localhost:$PORT"
        return NotificationCompat.Builder(this, LocalLLMApp.CHANNEL_SERVER)
            .setContentTitle("LocalLLM API Running")
            .setContentText("$address — loopback${if (lanMode) " + LAN" else " only"}")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ))
            .build()
    }
}
