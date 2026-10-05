package com.crosscast.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class StudioManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "StudioManager"
        const val LOCAL_CAMERA_ID = "local-camera"
        private const val OUT_W = 1280
        private const val OUT_H = 720
        private const val FRAME_INTERVAL_MS = 40L
    }

    private val sources = linkedMapOf<String, StudioSource>()
    private val scenes = linkedMapOf<String, Scene>()
    private val sourceBitmaps = mutableMapOf<String, Bitmap>()
    private val httpJobs = mutableMapOf<String, Job>()

    @Volatile
    var activeSceneId: String? = null
        private set

    @Volatile
    var lastStudioFrame: ByteArray? = null
        private set

    private var transitionJob: Job? = null

    init {
        sources[LOCAL_CAMERA_ID] = StudioSource.LocalCamera(LOCAL_CAMERA_ID, "Câmera local")

        // Cenas padrão
        scenes["camera_main"] = Scene("camera_main", "Câmera Principal", listOf(Layer(LOCAL_CAMERA_ID, LayerPosition.FULL)))

        activeSceneId = "camera_main"
        startRenderLoop()
    }

    fun onCameraFrame(jpeg: ByteArray) {
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.let { bmp ->
            sourceBitmaps[LOCAL_CAMERA_ID] = bmp
        }
    }

    fun registerSource(source: StudioSource) {
        sources[source.id] = source
        if (source is StudioSource.HttpMjpeg) {
            startHttpStream(source)
        }
    }

    private fun startHttpStream(source: StudioSource.HttpMjpeg) {
        httpJobs[source.id]?.cancel()
        httpJobs[source.id] = scope.launch {
            while (isActive) {
                try {
                    val client = java.net.URL(source.url).openConnection()
                    client.connectTimeout = 5000
                    client.readTimeout = 5000
                    val input = client.getInputStream()
                    val buffer = ByteArray(8192)
                    while (isActive) {
                        val bytes = input.read(buffer)
                        if (bytes > 0) {
                            val bmp = BitmapFactory.decodeByteArray(buffer, 0, bytes)
                            bmp?.let { sourceBitmaps[source.id] = it }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro HTTP stream: ${e.message}")
                }
                delay(1000)
            }
        }
    }

    private fun startRenderLoop() {
        scope.launch {
            while (isActive) {
                renderFrame()
                delay(FRAME_INTERVAL_MS)
            }
        }
    }

    private fun renderFrame() {
        val scene = scenes[activeSceneId] ?: return
        val output = Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)

        val paint = Paint().apply { isAntiAlias = true }

        for (layer in scene.layers) {
            val bmp = sourceBitmaps[layer.sourceId]
            if (bmp != null) {
                val destRect = when (layer.position) {
                    LayerPosition.FULL -> Rect(0, 0, OUT_W, OUT_H)
                    LayerPosition.TOP -> Rect(0, 0, OUT_W, OUT_H / 2)
                    LayerPosition.BOTTOM -> Rect(0, OUT_H / 2, OUT_W, OUT_H)
                    LayerPosition.LEFT -> Rect(0, 0, OUT_W / 2, OUT_H)
                    LayerPosition.RIGHT -> Rect(OUT_W / 2, 0, OUT_W, OUT_H)
                    LayerPosition.CENTER -> {
                        val scale = minOf(OUT_W.toFloat() / bmp.width, OUT_H.toFloat() / bmp.height)
                        val w = (bmp.width * scale).toInt()
                        val h = (bmp.height * scale).toInt()
                        Rect((OUT_W - w) / 2, (OUT_H - h) / 2, (OUT_W + w) / 2, (OUT_H + h) / 2)
                    }
                }
                canvas.drawBitmap(bmp, null, destRect, paint)
            }
        }

        val stream = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        lastStudioFrame = stream.toByteArray()
    }

    fun setActiveScene(sceneId: String) {
        if (scenes.containsKey(sceneId)) {
            activeSceneId = sceneId
        }
    }

    fun getStatus() = mapOf(
        "activeScene" to (activeSceneId ?: ""),
        "scenes" to scenes.keys.toList(),
        "sources" to sources.keys.toList()
    )
}

data class Scene(val id: String, val name: String, val layers: List<Layer>)
data class Layer(val sourceId: String, val position: LayerPosition)

enum class LayerPosition { FULL, TOP, BOTTOM, LEFT, RIGHT, CENTER }

sealed class StudioSource {
    abstract val id: String
    abstract val name: String

    data class LocalCamera(override val id: String, override val name: String) : StudioSource()
    data class HttpMjpeg(override val id: String, override val name: String, val url: String) : StudioSource()
}
