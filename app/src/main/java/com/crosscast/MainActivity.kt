package com.crosscast

import android.Manifest
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.crosscast.camera.CameraControlRegistry
import com.crosscast.camera.CameraManager 
import com.crosscast.ui.components.*
import com.crosscast.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// DEFINIÇÕES DE CLASSES DE DADOS
data class QuickLens(val label: String, val cameraId: String, val zoom: Float, val focalLength: Float)
data class CameraIdInfo(val id: String, val facing: String, val name: String, val focalLength: Float)

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var isServiceRunning by mutableStateOf(false)
    private var isFlashOn by mutableStateOf(false)
    private var ipAddress by mutableStateOf("0.0.0.0")
    private var activeLabel by mutableStateOf("1x")
    private var zoomSliderValue by mutableStateOf(1.0f)
    private val quickLenses = mutableStateListOf<QuickLens>()
    
    // Mapas para IDs específicos de câmeras traseiras
    private var backCameraIds = mutableMapOf<String, String>() // "0.6x", "1x", "3x", "5x"

    // Referência para o CameraManager para uso em toda a classe
    private lateinit var cameraManager: CameraManager

    private var currentCameraId by mutableStateOf<String?>(null)
    private var focusPoint by mutableStateOf<Offset?>(null)

    private fun performZoom(newValue: Float) {
        zoomSliderValue = newValue
        val (targetId, targetZoom) = when {
            newValue < 1.0f -> {
                val id = backCameraIds[".6x"] ?: backCameraIds["1x"] ?: "0"
                Pair(id, newValue / 0.6f)
            }
            newValue < 3.0f -> {
                val id = backCameraIds["1x"] ?: "0"
                Pair(id, newValue)
            }
            newValue < 5.0f -> {
                val id = backCameraIds["3x"] ?: backCameraIds["1x"] ?: "0"
                Pair(id, newValue / 3.0f)
            }
            else -> {
                val id = backCameraIds["5x"] ?: backCameraIds["3x"] ?: backCameraIds["1x"] ?: "0"
                val base = if (backCameraIds.containsKey("5x")) 5.0f else if (backCameraIds.containsKey("3x")) 3.0f else 1.0f
                Pair(id, newValue / base)
            }
        }

        if (targetId != currentCameraId) {
            currentCameraId = targetId
            cameraManager.switchCamera(targetId, targetZoom)
        } else {
            cameraManager.setZoom(targetZoom)
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            listCameras()
            ipAddress = getLocalIpAddress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        cameraManager = CameraManager(this)
        
        cameraManager.onFrameCaptured = { jpeg -> StaticCameraBridge.onFrame?.invoke(jpeg) }
        
        lifecycleScope.launch {
            CameraControlRegistry.commands.collectLatest { command ->
                val lens = quickLenses.find { it.cameraId == command.cameraId && it.zoom == command.zoom }
                activeLabel = lens?.label ?: "Custom"
                
                // Processar comando de flash se presente
                command.flash?.let {
                    isFlashOn = it
                    cameraManager.enableFlash(it)
                }

                // Calcular o valor real do zoom baseado no ID da câmera e zoomRatio do comando
                val baseZoom = when {
                    backCameraIds[".6x"] == command.cameraId -> 0.6f
                    backCameraIds["1x"] == command.cameraId -> 1.0f
                    backCameraIds["3x"] == command.cameraId -> 3.0f
                    backCameraIds["5x"] == command.cameraId -> 5.0f
                    else -> 1.0f
                }
                val totalZoom = baseZoom * command.zoom
                performZoom(totalZoom)
            }
        }
        
        setContent {
            // Atualizar IP automaticamente
            LaunchedEffect(Unit) {
                while(true) {
                    ipAddress = getLocalIpAddress()
                    kotlinx.coroutines.delay(5000)
                }
            }

            CrossCastTheme {
                BackHandler {
                    finish()
                }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showQrSheet by remember { mutableStateOf(false) }
                val serverUrl = if (ipAddress == "0.0.0.0") "SEM CONEXÃO" else "http://$ipAddress:8080"
                val qrCodeBitmap = remember(ipAddress) { if (ipAddress != "0.0.0.0") generateQRCode(serverUrl) else null }

                if (showQrSheet) {
                    ConnectionSheet(
                        url = serverUrl,
                        qrCode = qrCodeBitmap,
                        onDismiss = { showQrSheet = false },
                        onCopy = { copyToClipboard(serverUrl) },
                        sheetState = sheetState
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .then(if (showQrSheet) Modifier.blur(20.dp) else Modifier)
                ) {
                    // 1. Viewfinder (Fundo)
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                val initialId = backCameraIds["1x"] ?: "0"
                                currentCameraId = initialId
                                cameraManager.startCameraWithPreview(
                                    lifecycleOwner = this@MainActivity,
                                    executor = ContextCompat.getMainExecutor(ctx),
                                    previewView = this,
                                    cameraId = initialId
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { event ->
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    focusPoint = Offset(event.x, event.y)
                                    cameraManager.tapToFocus(event.x, event.y)
                                }
                                true
                            }
                    )

                    FocusIndicator(focusPoint) { focusPoint = null }

                    GridOverlay()

                    // 2. Barra Superior (Overlay)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Back button on the left
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back to menu",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Mostrar StatusPill apenas se NÃO estiver live para evitar redundância
                                    if (!isServiceRunning) {
                                        StatusPill(
                                            status = if (ipAddress != "0.0.0.0") "Online" else "Offline",
                                            isActive = ipAddress != "0.0.0.0",
                                            color = if (ipAddress != "0.0.0.0") AppleGreen else TextTertiary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(
                                        modifier = Modifier.clickable {
                                            if (ipAddress != "0.0.0.0") copyToClipboard(serverUrl)
                                        }
                                    ) {
                                        Text(
                                            text = "Live Camera",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${if (ipAddress != "0.0.0.0") "$ipAddress:8080" else ipAddress} · ${android.os.Build.MODEL}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isFlashOn = cameraManager.toggleFlash() }) {
                                        Icon(
                                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Flash",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { /* Settings */ }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Indicador LIVE central (Pill)
                        if (isServiceRunning) {
                            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 40.dp)) {
                                LivePill()
                            }
                        }
                    }

                    // 3. Controles Inferiores (Overlay)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Zoom Selector
                                ZoomSelector(
                                    options = listOf("0.6x", "1x", "2x", "5x", "10x"),
                                    selectedOption = activeLabel,
                                    onOptionSelected = { label ->
                                        activeLabel = label
                                        val newValue = when(label) {
                                            "0.6x" -> 0.6f
                                            "1x" -> 1.0f
                                            "2x" -> 2.0f
                                            "5x" -> 5.0f
                                            "10x" -> 10.0f
                                            else -> zoomSliderValue
                                        }
                                        performZoom(newValue)
                                    }
                                )

                                // Zoom Slider
                                ZoomSlider(
                                    value = zoomSliderValue,
                                    onValueChange = { newValue ->
                                        performZoom(newValue)
                                        activeLabel = "Custom"
                                    }
                                )

                                // Main Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconActionButton(
                                        icon = Icons.Default.QrCode,
                                        label = "Switcher",
                                        onClick = { showQrSheet = true }
                                    )

                                    RecordButton(
                                        isRecording = isServiceRunning,
                                        onClick = {
                                            if (isServiceRunning) CameraService.stop(this@MainActivity)
                                            else CameraService.start(this@MainActivity)
                                            isServiceRunning = !isServiceRunning
                                        }
                                    )

                                    IconActionButton(
                                        icon = Icons.Default.FlipCameraIos,
                                        label = "Camera",
                                        onClick = {
                                            if (activeLabel == "Selfie") {
                                                // Voltar para a principal (1x)
                                                activeLabel = "1x"
                                                performZoom(1.0f)
                                            } else {
                                                // Ir para Selfie
                                                val selfie = quickLenses.find { it.label == "Selfie" }
                                                if (selfie != null) {
                                                    activeLabel = "Selfie"
                                                    currentCameraId = selfie.cameraId
                                                    cameraManager.switchCamera(selfie.cameraId, 1.0f)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        checkPermissions()
        ipAddress = getLocalIpAddress()
        listCameras()
    }

    private fun shareAddress(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Endereço do Campilot: $url")
        }
        startActivity(Intent.createChooser(intent, "Compartilhar Endereço"))
    }

    private fun generateQRCode(text: String): Bitmap? {
        val width = 512
        val height = 512
        val writer = QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("URL", text))
        Toast.makeText(this, "Endereço copiado!", Toast.LENGTH_SHORT).show()
    }

    private fun getLocalIpAddress(): String {
        return getBestLocalIpAddress(this)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        requestPermissionLauncher.launch(permissions)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Composable
    fun GridOverlay() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 0.5.dp.toPx()
            val color = Color.White.copy(alpha = 0.15f)
            
            // Linhas Horizontais
            drawLine(color, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), strokeWidth)
            drawLine(color, Offset(0f, 2 * size.height / 3f), Offset(size.width, 2 * size.height / 3f), strokeWidth)
            
            // Linhas Verticais
            drawLine(color, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), strokeWidth)
            drawLine(color, Offset(2 * size.width / 3f, 0f), Offset(2 * size.width / 3f, size.height), strokeWidth)
        }
    }

    private fun listCameras() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
        try {
            val foundLenses = mutableListOf<QuickLens>()
            backCameraIds.clear()
            
            for (cameraId in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focal = focalLengths?.get(0) ?: 0f

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val label = when {
                        focal < 4.0f -> ".6x" // Ultra-wide (ex: 2.2mm)
                        focal < 7.5f -> "1x" // Main (ex: 6.3mm)
                        focal < 15.0f -> "3x" // Telephoto (ex: 7.9mm)
                        else -> "5x" // Periscope Telephoto (ex: 18.6mm)
                    }
                    
                    // Armazenar ID mapeado para o slider
                    backCameraIds[label] = cameraId
                    
                    foundLenses.add(QuickLens(label, cameraId, 1.0f, focal))
                    
                    if (label == "1x") foundLenses.add(QuickLens("2x", cameraId, 2.0f, focal + 0.1f))
                    
                    if (label == "5x") {
                        foundLenses.add(QuickLens("8x", cameraId, 1.6f, focal + 0.1f)) // 8x digital from 5x optical (5 * 1.6 = 8)
                        foundLenses.add(QuickLens("10x", cameraId, 2.0f, focal + 0.2f)) // 10x digital from 5x optical (5 * 2 = 10)
                    }
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (foundLenses.none { it.label == "Selfie" }) foundLenses.add(QuickLens("Selfie", cameraId, 1.0f, 999f))
                }
            }
            foundLenses.sortBy { it.focalLength }
            quickLenses.clear()
            quickLenses.addAll(foundLenses)
        } catch (e: Exception) { }
    }
}
