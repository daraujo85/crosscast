package com.campilot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.header
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import io.ktor.server.request.receive
import io.ktor.http.*
import com.campilot.data.AppDatabase
import com.campilot.data.Preset
import com.campilot.camera.CameraControlRegistry
import kotlinx.coroutines.flow.first
import io.ktor.server.response.respondBytesWriter
import io.ktor.utils.io.writeStringUtf8
import java.nio.ByteBuffer

class CameraService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private lateinit var database: AppDatabase
    
    @Volatile
    private var lastFrame: ByteArray? = null

    companion object {
        private const val CHANNEL_ID = "campilot_service_channel"
        private const val NOTIFICATION_ID = 101

        fun start(context: Context) {
            val intent = Intent(context, CameraService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CameraService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        StaticCameraBridge.onFrame = { frame -> lastFrame = frame }
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            if (database.presetDao().getAllPresets().first().isEmpty()) {
                val defaults = listOf(
                    Preset("wide", "Wide (Main)", "0", 1.0f),
                    Preset("ultrawide", "Ultra-wide", "2", 1.0f),
                    Preset("tele3x", "Tele 3x", "3", 1.0f),
                    Preset("tele10x", "Tele 10x", "4", 1.0f),
                    Preset("selfie", "Selfie", "1", 1.0f),
                    Preset("close-up", "Main Close-up", "0", 2.0f)
                )
                defaults.forEach { database.presetDao().insertPreset(it) }
            }
        }
        
        startKtorServer()
    }

    private fun startKtorServer() {
        serviceScope.launch {
            server = embeddedServer(Netty, port = 8080) {
                install(ContentNegotiation) {
                    json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
                }
                routing {
                    get("/") {
                        val html = """
                            <!DOCTYPE html>
                            <html lang="pt-br">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Campilot Pro - API Docs</title>
                                <style>
                                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #121212; color: #e0e0e0; line-height: 1.6; margin: 0; padding: 20px; }
                                    .container { max-width: 800px; margin: auto; background: #1e1e1e; padding: 30px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }
                                    h1 { color: #bb86fc; border-bottom: 2px solid #333; padding-bottom: 10px; }
                                    h2 { color: #03dac6; margin-top: 30px; }
                                    code { background: #333; padding: 2px 6px; border-radius: 4px; color: #ff7597; font-family: monospace; }
                                    .endpoint { background: #252525; padding: 15px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #bb86fc; }
                                    .method { font-weight: bold; color: #bb86fc; margin-right: 10px; }
                                    .url { color: #e0e0e0; font-family: monospace; }
                                    .params { margin-top: 10px; font-size: 0.9em; color: #aaa; }
                                    .example { background: #000; padding: 10px; border-radius: 4px; margin-top: 10px; overflow-x: auto; }
                                    a { color: #03dac6; text-decoration: none; }
                                    a:hover { text-decoration: underline; }
                                    .badge { background: #333; padding: 3px 8px; border-radius: 12px; font-size: 0.8em; margin-left: 10px; vertical-align: middle; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <h1>🚀 Campilot Pro API</h1>
                                    <p>Documentação dos endpoints disponíveis para controle remoto e streaming.</p>

                                    <div class="endpoint">
                                        <span class="method">GET</span><span class="url">/api/stream</span>
                                        <span class="badge">MJPEG Stream</span>
                                        <p>Visualização em tempo real da câmera no navegador ou OBS.</p>
                                        <div class="params">
                                            Parâmetros (Opcionais):<br>
                                            - <code>id</code>: ID da câmera (ex: 0, 1, 2)<br>
                                            - <code>zoom</code>: Valor decimal (ex: 1.0, 3.5)<br>
                                            - <code>flash</code>: true ou false
                                        </div>
                                        <div class="example">
                                            Ex para OBS: <code>http://[IP]:8080/api/stream?id=0&zoom=1.0&flash=true</code><br>
                                            <a href="/api/stream" target="_blank">Abrir Stream Padrão</a>
                                        </div>
                                    </div>

                                    <div class="endpoint">
                                        <span class="method">GET</span><span class="url">/api/presets</span>
                                        <p>Lista todos os presets de câmera configurados (ID, Lente, Zoom).</p>
                                    </div>

                                    <div class="endpoint">
                                        <span class="method">POST</span><span class="url">/api/presets/apply/{id}</span>
                                        <p>Aplica um preset específico pelo ID.</p>
                                        <div class="params">Exemplo: <code>POST /api/presets/apply/wide</code></div>
                                    </div>

                                    <div class="endpoint">
                                        <span class="method">POST</span><span class="url">/api/camera/select</span>
                                        <p>Controle manual de câmera e zoom.</p>
                                        <div class="params">
                                            Parâmetros (Query):<br>
                                            - <code>id</code>: ID da câmera (ex: 0, 1, 2)<br>
                                            - <code>zoom</code>: Valor decimal (ex: 1.0, 3.5)
                                        </div>
                                        <div class="example">Ex: <code>/api/camera/select?id=0&zoom=2.0</code></div>
                                    </div>

                                    <div class="endpoint">
                                        <span class="method">GET</span><span class="url">/status</span>
                                        <p>Verifica se o servidor está online e retorna informações do dispositivo.</p>
                                    </div>

                                    <footer style="margin-top: 40px; font-size: 0.8em; color: #666; text-align: center;">
                                        Campilot Pro v1.0 - S25 Ultra Optimized
                                    </footer>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()
                        call.respondText(html, ContentType.Text.Html)
                    }

                    get("/status") {
                        call.respond(mapOf("status" to "running", "device" to Build.MODEL))
                    }
                    
                    get("/api/presets") {
                        call.respond(database.presetDao().getAllPresets().first())
                    }

                    get("/api/stream") {
                        val id = call.request.queryParameters["id"]
                        val zoom = call.request.queryParameters["zoom"]?.toFloatOrNull()
                        val flash = call.request.queryParameters["flash"]?.toBooleanStrictOrNull()
                        
                        if (id != null || zoom != null || flash != null) {
                            CameraControlRegistry.emitCommand(id ?: "0", zoom ?: 1.0f, flash)
                        }

                        call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, max-age=0, must-revalidate")
                        call.respondBytesWriter(contentType = ContentType.parse("multipart/x-mixed-replace; boundary=--frame")) {
                            while (isActive) {
                                val frame = lastFrame
                                if (frame != null) {
                                    writeStringUtf8("--frame\r\n")
                                    writeStringUtf8("Content-Type: image/jpeg\r\n")
                                    writeStringUtf8("Content-Length: ${frame.size}\r\n\r\n")
                                    writeFully(ByteBuffer.wrap(frame))
                                    writeStringUtf8("\r\n")
                                    flush()
                                }
                                delay(40) // ~25 FPS
                            }
                        }
                    }

                    post("/api/camera/select") {
                        val id = call.request.queryParameters["id"] ?: "0"
                        val zoom = call.request.queryParameters["zoom"]?.toFloat() ?: 1.0f
                        val flash = call.request.queryParameters["flash"]?.toBooleanStrictOrNull()
                        CameraControlRegistry.emitCommand(id, zoom, flash)
                        call.respond(mapOf("status" to "success", "camera" to id, "zoom" to zoom, "flash" to flash))
                    }

                    post("/api/presets/apply/{id}") {
                        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val preset = database.presetDao().getPresetById(id)
                        if (preset != null) {
                            CameraControlRegistry.emitCommand(preset.cameraId, preset.zoomRatio)
                            call.respond(mapOf("applied" to preset))
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Preset not found")
                        }
                    }
                }
            }.start(wait = false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Campilot", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val ip = getBestLocalIpAddress(this)
        val serverUrl = "http://$ip:8080/api/stream"
        
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Campilot Streaming")
            .setContentText("MJPEG Server: $serverUrl")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(intent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop(500, 1000)
        serviceScope.cancel()
    }
}
