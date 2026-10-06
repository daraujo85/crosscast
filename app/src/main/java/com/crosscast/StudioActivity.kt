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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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

class StudioActivity : ComponentActivity() {

    private lateinit var autoSwitchManager: AutoSwitchManager
    private lateinit var cameraManager: com.crosscast.camera.CameraManager
    private lateinit var rtmpStreamer: RtmpStreamer

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            // Camera ready
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        autoSwitchManager = AutoSwitchManager(this, lifecycleScope)
        cameraManager = com.crosscast.camera.CameraManager(this)
        rtmpStreamer = RtmpStreamer(this)

        // Wire camera frames to RTMP streamer
        cameraManager.onFrameCaptured = { jpegData ->
            rtmpStreamer.encodeFrame(jpegData, isKeyframe = false)
        }

        autoSwitchManager.startHolyricsDetection()
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))

        // Track current camera ID
        var currentCameraId = "0"

        setContent {
            BackHandler { finish() }
            val obsState by autoSwitchManager.autoSwitchState.collectAsState()
            val streamState by rtmpStreamer.streamState.collectAsState()
            val streamAddress by rtmpStreamer.streamAddress.collectAsState()

            // Get camera ID from active scene
            val activeCameraId = obsState.activeScene?.cameraId ?: "0"

            // Switch camera when scene changes
            LaunchedEffect(activeCameraId) {
                if (activeCameraId != currentCameraId) {
                    currentCameraId = activeCameraId
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
                    // Camera will be switched via LaunchedEffect above
                },
                onTake = { autoSwitchManager.nextScene() },
                onToggleAutoSwitch = { autoSwitchManager.toggleAutoSwitch() },
                onToggleDetection = { if (obsState.detectionActive) autoSwitchManager.stopHolyricsDetection() else autoSwitchManager.startHolyricsDetection() },
                streamState = streamState,
                streamAddress = streamAddress,
                onToggleStream = {
                    android.util.Log.d("StudioActivity", "RTMP button clicked, streamState=$streamState")
                    rtmpStreamer.toggleStream()
                },
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
    onStreamAddressChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar ao menu",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = AppleGreen, fontWeight = FontWeight.Light)) {
                            append("Cross")
                        }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White, fontWeight = FontWeight.Light)) {
                            append("Cast")
                        }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append(" Studio")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Surface(color = AppleGreen, shape = RoundedCornerShape(4.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color.Red, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "LIVE", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Current: ${obsState.activeSceneId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        // Cenas com tratamento especial
        when (obsState.activeSceneId) {
            "camera_pip_holyrics" -> {
                SplitScene(
                    cameraManager = cameraManager,
                    lifecycleOwner = lifecycleOwner,
                    holyricsUrl = "http://192.168.31.231/view/widescreen",
                    activeCameraId = activeCameraId
                )
            }
            "holyrics_only" -> {
                HolyricsOnlyScene(
                    holyricsUrl = "http://192.168.31.231/view/widescreen"
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .aspectRatio(9f / 16f)
                            .border(3.dp, Color.Red.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
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
                                Text(text = obsState.activeScene?.name ?: "No scene", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Scenes", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(obsState.availableScenes) { scene ->
                val isActive = scene.id == obsState.activeSceneId
                Card(
                    modifier = Modifier
                        .width(90.dp)
                        .height(80.dp)
                        .clickable { onSceneSelect(scene.id) }
                        .border(
                            width = if (isActive) 3.dp else 1.dp,
                            color = if (isActive) AppleGreen else GlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) AppleGreen.copy(alpha = 0.25f) else Color(0xFF2A2A2A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.VideoLibrary,
                            contentDescription = if (isActive) "Ativo" else "Inativo",
                            tint = if (isActive) AppleGreen else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scene.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) AppleGreen else Color.White,
                            maxLines = 2,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onTake,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "TAKE",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)), shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleOBS, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Auto Switch", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("10s per scene", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Switch(
                    checked = obsState.autoSwitchEnabled,
                    onCheckedChange = { onToggleAutoSwitch() },
                    colors = SwitchDefaults.colors(checkedThumbColor = PurpleOBS, checkedTrackColor = PurpleOBS.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        StreamingCard(
            streamState = streamState,
            streamAddress = streamAddress,
            onToggleStream = onToggleStream,
            onAddressChange = onStreamAddressChange
        )

        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)), shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sources", color = Color.White, fontWeight = FontWeight.Bold)
                Row {
                    obsState.availableSources.forEach { source ->
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(if (source.hasSignal) AppleGreen else TextTertiary))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (obsState.isProjecting) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(color = PurpleOBS.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = PurpleOBS, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Projection → Camera + Lyrics", color = PurpleOBS, style = MaterialTheme.typography.bodySmall)
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
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .border(3.dp, PurpleOBS.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
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
                            webViewClient = object : WebViewClient() {
                                private var loadStartTime = System.currentTimeMillis()
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    // Only mark as failed if it's the main URL and at least 3 seconds have passed
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
                                    // Only mark as failed if it's the main URL (not sub-resources like .jpg)
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
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .border(3.dp, Color.Red.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
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
    activeCameraId: String = "0"
) {
    var holyricsOk by remember { mutableStateOf(true) }

    if (holyricsOk) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .aspectRatio(9f / 16f)
                .border(3.dp, Color.Red.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top: Camera preview (50%)
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
                // Bottom: Holyrics (50%)
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
                                webViewClient = object : WebViewClient() {
                                    private var loadStartTime = System.currentTimeMillis()
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        // Only mark as failed if it's the main URL and at least 3 seconds have passed
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
                                        // Only mark as failed if it's the main URL (not sub-resources like .jpg)
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
                        Text("HOLYRICS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    } else {
        // Holyrics falhou - mostra só camera fullscreen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .aspectRatio(9f / 16f)
                .border(3.dp, Color.Red.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
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

@Composable
fun StreamingCard(
    streamState: RtmpStreamer.StreamState,
    streamAddress: String,
    onToggleStream: () -> Unit,
    onAddressChange: (String) -> Unit
) {
    val isLive = streamState == RtmpStreamer.StreamState.LIVE
    val isConnecting = streamState == RtmpStreamer.StreamState.CONNECTING
    
    val statusColor = when (streamState) {
        RtmpStreamer.StreamState.LIVE -> Color.Red
        RtmpStreamer.StreamState.CONNECTING -> Color.Yellow
        RtmpStreamer.StreamState.ERROR -> Color.Red.copy(alpha = 0.5f)
        else -> TextTertiary
    }
    
    val statusText = when (streamState) {
        RtmpStreamer.StreamState.LIVE -> "LIVE"
        RtmpStreamer.StreamState.CONNECTING -> "CONNECTING..."
        RtmpStreamer.StreamState.ENCODING -> "ENCODING"
        RtmpStreamer.StreamState.ERROR -> "ERROR"
        RtmpStreamer.StreamState.OFFLINE -> "OFFLINE"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("RTMP Stream", color = Color.White, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(statusText, color = statusColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Button(
                    onClick = onToggleStream,
                    enabled = !isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) Color.Red else AppleGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLive) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLive) "STOP" else "START",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = streamAddress,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
