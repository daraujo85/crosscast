package com.crosscast

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.crosscast.studio.auto.AutoSwitchManager
import com.crosscast.studio.auto.AutoSwitchState
import com.crosscast.ui.theme.AppleGreen
import com.crosscast.ui.theme.GlassBorder
import com.crosscast.ui.theme.PurpleOBS
import com.crosscast.ui.theme.TextSecondary
import com.crosscast.ui.theme.TextTertiary
import com.crosscast.ui.components.LivePill
import kotlinx.coroutines.launch

class StudioActivity : ComponentActivity() {

    private lateinit var autoSwitchManager: AutoSwitchManager
    private lateinit var cameraManager: com.crosscast.camera.CameraManager
    private lateinit var rtmpStreamer: RtmpStreamer
    private lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            // Camera ready
        }
    }

    private var currentCameraId: String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        autoSwitchManager = AutoSwitchManager(this, lifecycleScope)
        cameraManager = com.crosscast.camera.CameraManager(this)
        rtmpStreamer = RtmpStreamer(this)
        settingsDataStore = SettingsDataStore(this)

        cameraManager.onFrameCaptured = { jpegData ->
            rtmpStreamer.encodeFrame(jpegData, isKeyframe = false)
        }

        autoSwitchManager.startHolyricsDetection()
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))

        setContent {
            BackHandler { finish() }
            val obsState by autoSwitchManager.autoSwitchState.collectAsState()
            val streamState by rtmpStreamer.streamState.collectAsState()

            // Load settings
            val scope = rememberCoroutineScope()
            val settings by settingsDataStore.streamSettings.collectAsState(
                initial = SettingsDataStore.StreamSettings(
                    rtmpUrl = SettingsDataStore.DEFAULT_RTMP_URL,
                    streamKey = "",
                    cameraId = "0",
                    width = 1280,
                    height = 720,
                    bitrate = 2500000
                )
            )

            // Configure RTMP streamer with settings
            LaunchedEffect(settings) {
                rtmpStreamer.configure(settings.width, settings.height, settings.bitrate)
            }

            var showSettings by remember { mutableStateOf(false) }

            val activeCameraId = obsState.activeScene?.cameraId ?: settings.cameraId
            var lastCameraId by remember { mutableStateOf(activeCameraId) }

            LaunchedEffect(activeCameraId) {
                if (activeCameraId != lastCameraId) {
                    lastCameraId = activeCameraId
                    cameraManager.switchCamera(activeCameraId)
                }
            }

            StudioContent(
                obsState = obsState,
                cameraManager = cameraManager,
                activeCameraId = activeCameraId,
                onBack = { finish() },
                onSceneSelect = { sceneId ->
                    autoSwitchManager.activateScene(sceneId)
                },
                onTake = { autoSwitchManager.nextScene() },
                onToggleAutoSwitch = { autoSwitchManager.toggleAutoSwitch() },
                onToggleDetection = { if (obsState.detectionActive) autoSwitchManager.stopHolyricsDetection() else autoSwitchManager.startHolyricsDetection() },
                streamState = streamState,
                streamAddress = settings.rtmpUrl,
                onToggleStream = {
                    val fullUrl = if (settings.streamKey.isNotEmpty()) {
                        "${settings.rtmpUrl}/${settings.streamKey}"
                    } else {
                        settings.rtmpUrl
                    }
                    rtmpStreamer.toggleStream(fullUrl)
                },
                showSettings = showSettings,
                onToggleSettings = { showSettings = !showSettings },
                settingsDataStore = settingsDataStore,
                currentSettings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSwitchManager.destroy()
        rtmpStreamer.release()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun StudioContent(
    obsState: AutoSwitchState,
    cameraManager: com.crosscast.camera.CameraManager? = null,
    activeCameraId: String = "0",
    onBack: () -> Unit = {},
    onSceneSelect: (String) -> Unit,
    onTake: () -> Unit,
    onToggleAutoSwitch: () -> Unit,
    onToggleDetection: () -> Unit,
    streamState: RtmpStreamer.StreamState = RtmpStreamer.StreamState.OFFLINE,
    streamAddress: String = RtmpStreamer.DEFAULT_RTMP_URL,
    onToggleStream: () -> Unit = {},
    showSettings: Boolean = false,
    onToggleSettings: () -> Unit = {},
    settingsDataStore: SettingsDataStore? = null,
    currentSettings: SettingsDataStore.StreamSettings? = null,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "CrossCast",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LivePill()
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onToggleSettings, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (showSettings) Icons.Default.Close else Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Scene indicator
            Text(
                text = obsState.activeSceneId ?: "No scene",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // PROGRAM / Preview Area - Max height
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (obsState.activeSceneId) {
                    "camera_pip_holyrics" -> {
                        SplitScene(
                            cameraManager = cameraManager,
                            lifecycleOwner = lifecycleOwner,
                            holyricsUrl = "http://192.168.31.231/view/widescreen",
                            activeCameraId = activeCameraId,
                            isProjecting = obsState.isProjecting
                        )
                    }
                    "holyrics_only" -> {
                        HolyricsOnlyScene(holyricsUrl = "http://192.168.31.231/view/widescreen")
                    }
                    else -> {
                        CameraProgramScene(
                            cameraManager = cameraManager,
                            lifecycleOwner = lifecycleOwner,
                            activeCameraId = activeCameraId,
                            sceneName = obsState.activeScene?.name ?: "No scene"
                        )
                    }
                }

                // Projection indicator overlay
                if (obsState.isProjecting) {
                    Surface(
                        color = PurpleOBS.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "PROJECTING",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Scene Strip - Compact horizontal scroll
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scenes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Tap to switch",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(obsState.availableScenes) { scene ->
                    val isActive = scene.id == obsState.activeSceneId
                    Card(
                        modifier = Modifier
                            .width(72.dp)
                            .height(56.dp)
                            .clickable { onSceneSelect(scene.id) }
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) AppleGreen else GlassBorder,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) AppleGreen.copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.VideoLibrary,
                                contentDescription = if (isActive) "Ativo" else "Inativo",
                                tint = if (isActive) AppleGreen else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = scene.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) AppleGreen else Color.White,
                                maxLines = 1,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // TAKE Button
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onTake,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "TAKE",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Bottom controls row
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto Switch Toggle
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurpleOBS,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = obsState.autoSwitchEnabled,
                            onCheckedChange = { onToggleAutoSwitch() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PurpleOBS,
                                checkedTrackColor = PurpleOBS.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                // Stream Toggle
                StreamButton(
                    streamState = streamState,
                    onToggleStream = onToggleStream,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Settings Panel (slide up from bottom)
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SettingsPanel(
                settingsDataStore = settingsDataStore,
                currentSettings = currentSettings,
                onClose = onToggleSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
            )
        }
    }
}

@Composable
fun StreamButton(
    streamState: RtmpStreamer.StreamState,
    onToggleStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = streamState == RtmpStreamer.StreamState.LIVE
    val isConnecting = streamState == RtmpStreamer.StreamState.CONNECTING

    val statusColor = when (streamState) {
        RtmpStreamer.StreamState.LIVE -> Color.Red
        RtmpStreamer.StreamState.CONNECTING -> Color.Yellow
        RtmpStreamer.StreamState.ERROR -> Color.Red.copy(alpha = 0.5f)
        else -> TextTertiary
    }

    Card(
        modifier = modifier.clickable(enabled = !isConnecting) { onToggleStream() },
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) Color.Red.copy(alpha = 0.2f) else Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLive) "LIVE" else "STREAM",
                    color = if (isLive) Color.Red else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (isLive) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint = if (isLive) Color.Red else AppleGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsPanel(
    settingsDataStore: SettingsDataStore?,
    currentSettings: SettingsDataStore.StreamSettings?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rtmpUrl by remember { mutableStateOf(currentSettings?.rtmpUrl ?: SettingsDataStore.DEFAULT_RTMP_URL) }
    var streamKey by remember { mutableStateOf(currentSettings?.streamKey ?: "") }
    var cameraId by remember { mutableStateOf(currentSettings?.cameraId ?: "0") }
    var bitrate by remember { mutableFloatStateOf((currentSettings?.bitrate ?: 2500000).toFloat()) }
    var resolution by remember { mutableStateOf(currentSettings?.width ?: 1280) }

    LaunchedEffect(currentSettings) {
        currentSettings?.let {
            rtmpUrl = it.rtmpUrl
            streamKey = it.streamKey
            cameraId = it.cameraId
            bitrate = it.bitrate.toFloat()
            resolution = it.width
        }
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Stream Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // RTMP URL
            Text("RTMP Server URL", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = rtmpUrl,
                onValueChange = { rtmpUrl = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PurpleOBS,
                    unfocusedBorderColor = GlassBorder
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stream Key
            Text("Stream Key (optional)", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = streamKey,
                onValueChange = { streamKey = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PurpleOBS,
                    unfocusedBorderColor = GlassBorder
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Camera Selection
            Text("Camera", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("0" to "Back", "1" to "Front", "2" to "Wide").forEach { (id, label) ->
                    Card(
                        modifier = Modifier
                            .clickable { cameraId = id }
                            .then(
                                if (cameraId == id) Modifier.border(2.dp, AppleGreen, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (cameraId == id) AppleGreen.copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            label,
                            color = if (cameraId == id) AppleGreen else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resolution
            Text("Resolution: ${resolution}p", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(720 to "720p", 1080 to "1080p").forEach { (res, label) ->
                    Card(
                        modifier = Modifier
                            .clickable { resolution = res }
                            .then(
                                if (resolution == res) Modifier.border(2.dp, PurpleOBS, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (resolution == res) PurpleOBS.copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            label,
                            color = if (resolution == res) PurpleOBS else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bitrate
            Text("Bitrate: ${bitrate.toInt() / 1000} kbps", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = bitrate,
                onValueChange = { bitrate = it },
                valueRange = 1_000_000f..6_000_000f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = PurpleOBS,
                    activeTrackColor = PurpleOBS
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    scope.launch {
                        settingsDataStore?.setRtmpUrl(rtmpUrl)
                        settingsDataStore?.setStreamKey(streamKey)
                        settingsDataStore?.setCameraId(cameraId)
                        settingsDataStore?.setResolution(resolution, resolution * 9 / 16)
                        settingsDataStore?.setBitrate(bitrate.toInt())
                    }
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraProgramScene(
    cameraManager: com.crosscast.camera.CameraManager?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    activeCameraId: String,
    sceneName: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            cameraManager?.startCameraWithPreview(
                                lifecycleOwner = lifecycleOwner,
                                executor = ContextCompat.getMainExecutor(ctx),
                                previewView = this,
                                cameraId = activeCameraId
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (cameraManager == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0A0A)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aguardando câmera...", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    color = Color.Red.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("PROGRAM", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(text = sceneName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun HolyricsOnlyScene(holyricsUrl: String) {
    var holyricsOk by remember { mutableStateOf(true) }

    if (holyricsOk) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, PurpleOBS.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webViewClient = object : WebViewClient() {
                                private var loadStartTime = System.currentTimeMillis()
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    if (failingUrl == holyricsUrl && System.currentTimeMillis() - loadStartTime >= 3000) {
                                        holyricsOk = false
                                    }
                                }
                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    errorResponse: android.webkit.WebResourceResponse?
                                ) {
                                    val url = request?.url?.toString()
                                    if (url == holyricsUrl && errorResponse?.statusCode == 404 && System.currentTimeMillis() - loadStartTime >= 3000) {
                                        holyricsOk = false
                                    }
                                }
                            }
                            loadUrl(holyricsUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = PurpleOBS.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("HOLYRICS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Holyrics offline",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun SplitScene(
    cameraManager: com.crosscast.camera.CameraManager?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    holyricsUrl: String,
    activeCameraId: String = "0",
    isProjecting: Boolean = true
) {
    var holyricsOk by remember { mutableStateOf(true) }

    if (!isProjecting) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            cameraManager?.startCameraWithPreview(
                                lifecycleOwner = lifecycleOwner,
                                executor = ContextCompat.getMainExecutor(ctx),
                                previewView = this,
                                cameraId = activeCameraId
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Red.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("CAM", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        return
    }

    if (holyricsOk) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                cameraManager?.startCameraWithPreview(
                                    lifecycleOwner = lifecycleOwner,
                                    executor = ContextCompat.getMainExecutor(ctx),
                                    previewView = this,
                                    cameraId = activeCameraId
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Red.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CAM", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                webViewClient = object : WebViewClient() {
                                    private var loadStartTime = System.currentTimeMillis()
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        if (failingUrl == holyricsUrl && System.currentTimeMillis() - loadStartTime >= 3000) {
                                            holyricsOk = false
                                        }
                                    }
                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        errorResponse: android.webkit.WebResourceResponse?
                                    ) {
                                        val url = request?.url?.toString()
                                        if (url == holyricsUrl && errorResponse?.statusCode == 404 && System.currentTimeMillis() - loadStartTime >= 3000) {
                                            holyricsOk = false
                                        }
                                    }
                                }
                                loadUrl(holyricsUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = PurpleOBS.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("LYRICS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            cameraManager?.startCameraWithPreview(
                                lifecycleOwner = lifecycleOwner,
                                executor = ContextCompat.getMainExecutor(ctx),
                                previewView = this,
                                cameraId = activeCameraId
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Red.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("CAM", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Holyrics offline",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
