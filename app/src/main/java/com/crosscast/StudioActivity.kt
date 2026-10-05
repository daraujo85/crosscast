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
import androidx.compose.runtime.Composable
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
        autoSwitchManager.startHolyricsDetection()
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        setContent {
            BackHandler { finish() }
            val obsState by autoSwitchManager.autoSwitchState.collectAsState()
            StudioContent(
                obsState = obsState,
                cameraManager = cameraManager,
                onBack = { finish() },
                onSceneSelect = { autoSwitchManager.activateScene(it) },
                onTake = { autoSwitchManager.nextScene() },
                onToggleAutoSwitch = { autoSwitchManager.toggleAutoSwitch() },
                onToggleDetection = { if (obsState.detectionActive) autoSwitchManager.stopHolyricsDetection() else autoSwitchManager.startHolyricsDetection() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSwitchManager.destroy()
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
    onBack: () -> Unit = {},
    onSceneSelect: (String) -> Unit,
    onTake: () -> Unit,
    onToggleAutoSwitch: () -> Unit,
    onToggleDetection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header mais compacto
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

        // Cena split: camera em cima + Holyrics embaixo
        if (obsState.activeSceneId == "camera_pip_holyrics") {
            SplitScene(
                cameraManager = cameraManager,
                lifecycleOwner = lifecycleOwner,
                holyricsUrl = "http://192.168.31.231/view/widescreen"
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(3.dp, Color.Red.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                cameraManager?.startCameraWithPreview(
                                    lifecycleOwner = lifecycleOwner,
                                    executor = ContextCompat.getMainExecutor(ctx),
                                    previewView = this,
                                    cameraId = "0"
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
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Aguardando câmera...",
                                color = Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Surface(
                        color = Color.Red.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("PROGRAM", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(text = obsState.activeScene?.name ?: "No scene", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Scenes", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(obsState.availableScenes) { scene ->
                val isActive = scene.id == obsState.activeSceneId
                Card(
                    modifier = Modifier
                        .width(80.dp)
                        .height(70.dp)
                        .clickable { onSceneSelect(scene.id) }
                        .border(2.dp, if (isActive) AppleGreen else GlassBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = if (isActive) AppleGreen.copy(alpha = 0.2f) else Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = if (isActive) AppleGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scene.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                fontSize = 10.sp
                            )
                        }
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("TAKE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
fun SplitScene(
    cameraManager: com.crosscast.camera.CameraManager?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    holyricsUrl: String
) {
    var holyricsOk by remember { mutableStateOf(true) }

    if (holyricsOk) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
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
                                    cameraId = "0"
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
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        holyricsOk = false
                                    }
                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        errorResponse: android.webkit.WebResourceResponse?
                                    ) {
                                        if (request?.url?.toString()?.contains(".jpg") == true ||
                                            errorResponse?.statusCode == 404) {
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
                .height(220.dp)
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
                                cameraId = "0"
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
