package com.crosscast.studio.auto

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object AutoSwitchManager {

    private const val TAG = "ObsManager"

    private var _context: Context? = null
    private var _scope: CoroutineScope? = null
    private val _autoSwitchState = MutableStateFlow(AutoSwitchState())
    val autoSwitchState: StateFlow<AutoSwitchState> = _autoSwitchState.asStateFlow()

    fun initialize(context: Context, scope: CoroutineScope) {
        _context = context.applicationContext
        _scope = scope
        initializeDefaultScenes()
        initializeDefaultSources()
    }

    val isInitialized: Boolean get() = _context != null && _scope != null

    private val scenes = ConcurrentHashMap<String, AutoSwitchScene>()
    private val sources = ConcurrentHashMap<String, AutoSwitchSource>()

    private var holyricsJob: Job? = null
    private var autoSwitchJob: Job? = null
    private var timerSwitchJob: Job? = null
    private var lastProjectionHash = 0
    private var lastProjectionFrame: ByteArray? = null

    var holyricsUrl = "http://192.168.1.100:4000/live"
    var autoSwitchInterval = 5000L
    var timerSwitchEnabled = false
    var timerSwitchSeconds = 10L

    private fun initializeDefaultScenes() {
        scenes["camera_main"] = AutoSwitchScene("camera_main", "Câmera Principal", listOf(AutoSwitchLayer(AutoSwitchLayerType.CAMERA, AutoSwitchLayerPosition.FULL)), "0", 1.0f)
        scenes["camera_wide"] = AutoSwitchScene("camera_wide", "Câmera Wide", listOf(AutoSwitchLayer(AutoSwitchLayerType.CAMERA, AutoSwitchLayerPosition.FULL)), "2", 1.0f)
        scenes["camera_telephoto"] = AutoSwitchScene("camera_telephoto", "Câmera Tele", listOf(AutoSwitchLayer(AutoSwitchLayerType.CAMERA, AutoSwitchLayerPosition.FULL)), "4", 1.0f)
        scenes["camera_pip_holyrics"] = AutoSwitchScene("camera_pip_holyrics", "Câmera + Projeção", listOf(AutoSwitchLayer(AutoSwitchLayerType.CAMERA, AutoSwitchLayerPosition.FULL), AutoSwitchLayer(AutoSwitchLayerType.HOLYRICS_OVERLAY, AutoSwitchLayerPosition.BOTTOM, 33f)), "0", 1.0f)
        scenes["holyrics_only"] = AutoSwitchScene("holyrics_only", "Somente Projeção", listOf(AutoSwitchLayer(AutoSwitchLayerType.HOLYRICS_OVERLAY, AutoSwitchLayerPosition.FULL)))
        _autoSwitchState.value = _autoSwitchState.value.copy(availableScenes = scenes.values.toList(), activeScene = scenes["camera_main"]!!)
    }

    private fun initializeDefaultSources() {
        sources["camera"] = AutoSwitchSource("camera", "Câmera", AutoSwitchSourceType.CAMERA, "", false, "", false, false, AutoSwitchSplitPosition.BOTTOM)
        sources["holyrics"] = AutoSwitchSource("holyrics", "Holyrics", AutoSwitchSourceType.HOLYRICS, "http://192.168.1.100:4000/live", true, "http://192.168.1.100:4000/live", true, true, AutoSwitchSplitPosition.BOTTOM)
        _autoSwitchState.value = _autoSwitchState.value.copy(availableSources = sources.values.toList())
    }

    fun activateScene(sceneId: String): Boolean {
        val scene = scenes[sceneId] ?: return false
        _autoSwitchState.value = _autoSwitchState.value.copy(activeScene = scene, activeSceneId = sceneId)
        Log.d(TAG, "Cena: ${scene.name}")
        return true
    }

    fun nextScene() {
        val list = scenes.values.toList()
        val idx = list.indexOfFirst { it.id == _autoSwitchState.value.activeSceneId }
        activateScene(list[(idx + 1) % list.size].id)
    }

    fun previousScene() {
        val list = scenes.values.toList()
        val idx = list.indexOfFirst { it.id == _autoSwitchState.value.activeSceneId }
        activateScene(list[if (idx <= 0) list.size - 1 else idx - 1].id)
    }

    fun startSignalDetection(sourceId: String) {
        val source = sources[sourceId] ?: return
        _scope!!.launch {
            while (isActive) {
                try {
                    val hasSignal = detectSignal(source.signalDetectionUrl)
                    val updatedSource = source.copy(hasSignal = hasSignal)
                    sources[sourceId] = updatedSource
                    if (hasSignal && source.autoSwitchOnSignal) {
                        val sceneWithSource = scenes.values.find { scene -> 
                            scene.layers.any { layer -> 
                                when (layer.type) {
                                    AutoSwitchLayerType.CAMERA -> source.type == AutoSwitchSourceType.CAMERA
                                    AutoSwitchLayerType.HOLYRICS_OVERLAY -> source.type == AutoSwitchSourceType.HOLYRICS
                                    else -> false
                                }
                            }
                        }
                        sceneWithSource?.let { activateScene(it.id) }
                    }
                    _autoSwitchState.value = _autoSwitchState.value.copy(availableSources = sources.values.toList())
                } catch (e: Exception) { Log.e(TAG, "Signal detect error: ${e.message}") }
                delay(1000)
            }
        }
    }

    private fun detectSignal(url: String): Boolean {
        if (url.isEmpty()) return false
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) { false }
    }

    fun startHolyricsDetection() {
        if (holyricsJob?.isActive == true) return
        holyricsJob = _scope!!.launch {
            while (isActive) {
                try {
                    val hasProj = detectHolyrics()
                    val wasProj = _autoSwitchState.value.isProjecting
                    _autoSwitchState.value = _autoSwitchState.value.copy(isProjecting = hasProj)
                    if (hasProj && !wasProj) {
                        _autoSwitchState.value = _autoSwitchState.value.copy(lastEvent = "projection_start")
                        activateScene("camera_pip_holyrics")
                    } else if (!hasProj && wasProj) {
                        _autoSwitchState.value = _autoSwitchState.value.copy(lastEvent = "projection_end")
                        activateScene("camera_main")
                    }
                } catch (e: Exception) { Log.e(TAG, "Erro: ${e.message}") }
                delay(1000)
            }
        }
        _autoSwitchState.value = _autoSwitchState.value.copy(detectionActive = true)
    }

    fun stopHolyricsDetection() {
        holyricsJob?.cancel(); holyricsJob = null
        _autoSwitchState.value = _autoSwitchState.value.copy(detectionActive = false)
    }

    private suspend fun detectHolyrics(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(holyricsUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            val data = conn.inputStream.readBytes()
            conn.disconnect()
            if (data.isNotEmpty()) {
                val h = data.contentHashCode()
                if (kotlin.math.abs(h - lastProjectionHash) > 1000) { 
                    lastProjectionHash = h
                    lastProjectionFrame = data
                    return@withContext true 
                }
            }
            false
        } catch (e: Exception) { Log.e(TAG, "Detect err: ${e.message}"); false }
    }

    fun startTimerSwitch() {
        if (timerSwitchJob?.isActive == true) return
        timerSwitchEnabled = true
        timerSwitchJob = _scope!!.launch {
            val sceneList = scenes.values.toList()
            var idx = 0
            while (isActive && timerSwitchEnabled) {
                if (!_autoSwitchState.value.isProjecting) {
                    activateScene(sceneList[idx].id)
                    idx = (idx + 1) % sceneList.size
                }
                delay(timerSwitchSeconds * 1000)
            }
        }
        _autoSwitchState.value = _autoSwitchState.value.copy(timerSwitchEnabled = true)
    }

    fun stopTimerSwitch() {
        timerSwitchEnabled = false
        timerSwitchJob?.cancel(); timerSwitchJob = null
        _autoSwitchState.value = _autoSwitchState.value.copy(timerSwitchEnabled = false)
    }

    fun setTimerInterval(seconds: Long) { timerSwitchSeconds = seconds }

    fun startAutoSwitch() {
        if (autoSwitchJob?.isActive == true) return
        _autoSwitchState.value = _autoSwitchState.value.copy(autoSwitchEnabled = true)
        autoSwitchJob = _scope!!.launch {
            val cams = listOf("camera_main", "camera_wide", "camera_telephoto")
            var idx = 0
            while (isActive && _autoSwitchState.value.autoSwitchEnabled) {
                if (!_autoSwitchState.value.isProjecting) { 
                    activateScene(cams[idx]); 
                    idx = (idx + 1) % cams.size 
                }
                delay(autoSwitchInterval)
            }
        }
    }

    fun stopAutoSwitch() { 
        autoSwitchJob?.cancel(); 
        autoSwitchJob = null; 
        _autoSwitchState.value = _autoSwitchState.value.copy(autoSwitchEnabled = false) 
    }

    fun toggleAutoSwitch() = if (_autoSwitchState.value.autoSwitchEnabled) stopAutoSwitch() else startAutoSwitch()

    fun updateSource(source: AutoSwitchSource) { 
        sources[source.id] = source
        _autoSwitchState.value = _autoSwitchState.value.copy(availableSources = sources.values.toList())
    }

    fun getSource(id: String): AutoSwitchSource? = sources[id]

    fun addScene(scene: AutoSwitchScene) {
        scenes[scene.id] = scene
        _autoSwitchState.value = _autoSwitchState.value.copy(availableScenes = scenes.values.toList())
    }

    fun removeScene(sceneId: String) {
        if (sceneId.startsWith("camera_") || sceneId == "holyrics_only") return
        scenes.remove(sceneId)
        _autoSwitchState.value = _autoSwitchState.value.copy(availableScenes = scenes.values.toList())
    }

    fun destroy() { stopHolyricsDetection(); stopAutoSwitch(); stopTimerSwitch() }

    fun getProjectionFrame(): ByteArray? = lastProjectionFrame
}

data class AutoSwitchState(
    val activeScene: AutoSwitchScene? = null,
    val activeSceneId: String = "camera_main",
    val availableScenes: List<AutoSwitchScene> = emptyList(),
    val availableSources: List<AutoSwitchSource> = emptyList(),
    val isProjecting: Boolean = false,
    val autoSwitchEnabled: Boolean = false,
    val timerSwitchEnabled: Boolean = false,
    val detectionActive: Boolean = false,
    val lastEvent: String? = null,
    val streamingToRtmp: Boolean = false,
    val rtmpUrl: String = ""
)

data class AutoSwitchScene(
    val id: String,
    val name: String,
    val layers: List<AutoSwitchLayer>,
    val cameraId: String = "0",
    val zoom: Float = 1.0f,
    val transitionTimeMs: Int = 300
)

data class AutoSwitchLayer(
    val type: AutoSwitchLayerType,
    val position: AutoSwitchLayerPosition,
    val heightPercent: Float = 100f,
    val widthPercent: Float = 100f
)

enum class AutoSwitchLayerType { CAMERA, HOLYRICS_OVERLAY, IMAGE, COLOR, BROWSER }
enum class AutoSwitchLayerPosition { FULL, TOP, BOTTOM, LEFT, RIGHT, CENTER }

data class AutoSwitchSource(
    val id: String,
    val name: String,
    val type: AutoSwitchSourceType,
    val url: String,
    val signalDetectionEnabled: Boolean,
    val signalDetectionUrl: String,
    val autoSwitchOnSignal: Boolean,
    val splitMode: Boolean,
    val splitPosition: AutoSwitchSplitPosition,
    val hasSignal: Boolean = false
)

enum class AutoSwitchSourceType { CAMERA, HOLYRICS, IMAGE, BROWSER, RTMP }
enum class AutoSwitchSplitPosition { TOP, BOTTOM, LEFT, RIGHT }
