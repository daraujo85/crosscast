package com.crosscast

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
import com.crosscast.camera.CameraControlRegistry
import com.crosscast.studio.StudioManager
import com.crosscast.studio.auto.AutoSwitchManager
import kotlinx.coroutines.flow.first
import io.ktor.server.response.respondBytesWriter
import io.ktor.utils.io.writeStringUtf8
import java.nio.ByteBuffer
import android.content.pm.ServiceInfo

class CameraService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    
    @Volatile
    private var lastFrame: ByteArray? = null

    private lateinit var studioManager: StudioManager

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
        studioManager = StudioManager(applicationContext, serviceScope)
        StaticCameraBridge.onFrame = { frame ->
            lastFrame = frame
            studioManager.onCameraFrame(frame)
        }

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
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
                                <title>Campilot Pro - Painel</title>
                                <link rel="preconnect" href="https://fonts.googleapis.com">
                                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                                <style>
                                    :root {
                                        --bg-color: #e0e5ec;
                                        --text-color: #3c4b64;
                                        --text-light: #99a4b6;
                                        --shadow-dark: #a3b1c6;
                                        --shadow-light: #ffffff;
                                        --accent-color: #D4AF37;
                                    }
                                    body {
                                        font-family: 'Inter', sans-serif;
                                        background-color: var(--bg-color);
                                        color: var(--text-color);
                                        margin: 0;
                                        padding: 30px;
                                        display: flex;
                                        justify-content: center;
                                        align-items: flex-start;
                                        min-height: 100vh;
                                    }
                                    .container {
                                        width: 100%;
                                        max-width: 800px;
                                    }
                                    .header {
                                        text-align: center;
                                        margin-bottom: 50px;
                                    }
                                    .header img {
                                        height: 45px;
                                        margin-bottom: 15px;
                                        filter: drop-shadow(2px 2px 3px var(--shadow-dark));
                                    }
                                    .header h1 {
                                        font-size: 1.8rem;
                                        font-weight: 600;
                                        margin-bottom: 5px;
                                    }
                                    .header p {
                                        color: var(--text-light);
                                        font-size: 1rem;
                                    }
                                    .card {
                                        background-color: var(--bg-color);
                                        border-radius: 20px;
                                        padding: 25px;
                                        margin-bottom: 30px;
                                        box-shadow: 6px 6px 12px var(--shadow-dark), -6px -6px 12px var(--shadow-light);
                                    }
                                    .card h2 {
                                        margin: 0 0 15px 0;
                                        font-size: 1.2rem;
                                        font-weight: 600;
                                        color: var(--text-color);
                                    }
                                    .url {
                                        font-family: monospace;
                                        font-size: 1rem;
                                        font-weight: 500;
                                        color: #6d7a90;
                                        background-color: var(--bg-color);
                                        padding: 15px;
                                        border-radius: 10px;
                                        box-shadow: inset 2px 2px 5px var(--shadow-dark), inset -2px -2px 5px var(--shadow-light);
                                        word-break: break-all;
                                    }
                                    .description {
                                        margin-bottom: 15px;
                                        color: var(--text-light);
                                        line-height: 1.6;
                                    }
                                    .footer {
                                        text-align: center;
                                        margin-top: 40px;
                                        font-size: 0.9rem;
                                        color: var(--text-light);
                                    }
                                    .footer a {
                                        color: var(--text-color);
                                        text-decoration: none;
                                        font-weight: 600;
                                    }
                                    .footer a:hover {
                                        text-decoration: underline;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <img src="https://devsync.com.br/wp-content/uploads/2023/05/devsync.png" alt="DevSync Logo">
                                        <h1>Painel de Controle Campilot</h1>
                                        <p>Guia de uso para OBS e outros softwares</p>
                                    </div>

                                    <div class="card">
                                        <h2>🌟 Guia Prático para OBS</h2>
                                        <p class="description">
                                            O poder do Campilot está em usar URLs diferentes para criar múltiplas "câmeras virtuais" no OBS. Cada URL pode especificar uma lente e um nível de zoom, permitindo que você alterne entre cenas como em uma produção multi-câmera profissional, usando apenas um celular.
                                        </p>
                                        <p class="description">
                                            No OBS, adicione uma nova fonte do tipo <strong>"Navegador" (Browser)</strong> e cole um dos exemplos de URL abaixo.
                                        </p>
                                    </div>

                                    <div class="card">
                                        <h2>Plano Geral (Câmera Principal)</h2>
                                        <p class="description">Use esta cena para a visão principal do seu cenário, palco ou apresentador.</p>
                                        <div class="url">http://[SEU.IP]:8080/api/stream?id=0&zoom=1.0</div>
                                    </div>

                                    <div class="card">
                                        <h2>Close-up (Zoom 10x)</h2>
                                        <p class="description">Perfeito para focar em detalhes, no rosto do apresentador ou em um produto. Este exemplo usa a lente Telefoto 5x com um zoom digital de 2x, resultando em 10x.</p>
                                        <div class="url">http://[SEU.IP]:8080/api/stream?id=4&zoom=2.0</div>
                                    </div>
                                    
                                    <div class="card">
                                        <h2>Plano Americano (Zoom 2x)</h2>
                                        <p class="description">Ideal para um enquadramento médio, pegando o apresentador da cintura para cima.</p>
                                        <div class="url">http://[SEU.IP]:8080/api/stream?id=0&zoom=2.0</div>
                                    </div>

                                    <div class="card">
                                        <h2>Visão Ampla (Ultra-Wide)</h2>
                                        <p class="description">Use a lente Ultra-Wide para capturar todo o ambiente, a plateia ou um cenário expansivo.</p>
                                        <div class="url">http://[SEU.IP]:8080/api/stream?id=2&zoom=1.0</div>
                                    </div>
                                     <div class="footer">
                                        Desenvolvido por <a href="https://devsync.com.br" target="_blank">DevSync</a>
                                    </div>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()
                        call.respondText(html, ContentType.Text.Html)
                    }

                    get("/status") {
                        call.respond(mapOf("status" to "running", "device" to Build.MODEL))
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
        val serverUrl = "http://$ip/api/stream"
        
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
