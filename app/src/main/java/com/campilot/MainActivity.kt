package com.campilot

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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.campilot.camera.CameraControlRegistry
import com.campilot.camera.CameraManager as CampilotCameraManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// DEFINIÇÕES DE CLASSES DE DADOS
data class QuickLens(val label: String, val cameraId: String, val zoom: Float, val focalLength: Float)
data class CameraIdInfo(val id: String, val facing: String, val name: String, val focalLength: Float)

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
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
    private lateinit var cameraManager: CampilotCameraManager
    
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
        cameraManager = CampilotCameraManager(this)
        
        cameraManager.onFrameCaptured = { jpeg -> StaticCameraBridge.onFrame?.invoke(jpeg) }
        
        lifecycleScope.launch {
            CameraControlRegistry.commands.collectLatest { command ->
                val lens = quickLenses.find { it.cameraId == command.cameraId && it.zoom == command.zoom }
                activeLabel = lens?.label ?: "Custom"
                if (lens != null) {
                    val baseZoom = when(lens.label) {
                        ".6x" -> 0.6f
                        "1x" -> 1.0f
                        "2x" -> 2.0f
                        "3x" -> 3.0f
                        "5x" -> 5.0f
                        "10x" -> 10.0f
                        else -> command.zoom
                    }
                    zoomSliderValue = baseZoom
                }
                
                // Processar comando de flash se presente
                command.flash?.let {
                    isFlashOn = it
                    cameraManager.enableFlash(it)
                }

                cameraManager.switchCamera(command.cameraId, command.zoom)
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

            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF17131F),
                                    Color(0xFF121212),
                                    Color(0xFF0C0C0C)
                                )
                            )
                        )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                        val configuration = LocalConfiguration.current
                        val previewAspectRatio = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            9f / 16f
                        } else {
                            16f / 9f
                        }

                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Campilot",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Streaming local e troca de lente do S25 Ultra",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB0B0B0)
                            )

                            HeaderSection(ipAddress, isServiceRunning)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
                                border = BorderStroke(1.dp, Color(0xFF313131)),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(previewAspectRatio)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PreviewView(ctx).apply {
                                                scaleType = PreviewView.ScaleType.FIT_CENTER
                                                cameraManager.startCameraWithPreview(
                                                    lifecycleOwner = this@MainActivity,
                                                    executor = ContextCompat.getMainExecutor(ctx),
                                                    previewView = this,
                                                    cameraId = quickLenses.find { it.label == "1x" }?.cameraId ?: "0"
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInteropFilter { event ->
                                                if (event.action == MotionEvent.ACTION_DOWN) {
                                                    cameraManager.tapToFocus(event.x, event.y)
                                                }
                                                true
                                            }
                                    )

                                    Surface(
                                        color = Color.Black.copy(alpha = 0.50f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Toque para focar",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { isFlashOn = cameraManager.toggleFlash() },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Flash",
                                            tint = if (isFlashOn) Color.Yellow else Color.White
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                                border = BorderStroke(1.dp, Color(0xFF2A2A2A)),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Zoom Preciso",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = String.format("%.1fx", zoomSliderValue),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color(0xFFFFEB3B),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Slider(
                                        value = zoomSliderValue,
                                        onValueChange = { newValue ->
                                            zoomSliderValue = newValue
                                            // Lógica robusta para mapear o slider apenas para câmeras traseiras identificadas
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
                                                    Pair(id, newValue / 5.0f)
                                                }
                                            }
                                            cameraManager.switchCamera(targetId, targetZoom)
                                            activeLabel = "Custom"
                                        },
                                        valueRange = 0.6f..10.0f,
                                        steps = 94, // (10 - 0.6) / 0.1 = 94
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFFEB3B),
                                            activeTrackColor = Color(0xFFFFEB3B),
                                            inactiveTrackColor = Color(0xFF353535)
                                        )
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                                border = BorderStroke(1.dp, Color(0xFF2A2A2A)),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Seletor de lente",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        quickLenses.forEach { lens ->
                                            FilterChip(
                                                selected = activeLabel == lens.label,
                                                onClick = {
                                                    activeLabel = lens.label
                                                    // Sincronizar slider com o botão clicado
                                                    zoomSliderValue = when(lens.label) {
                                                        ".6x" -> 0.6f
                                                        "1x" -> 1.0f
                                                        "2x" -> 2.0f
                                                        "3x" -> 3.0f
                                                        "5x" -> 5.0f
                                                        "10x" -> 10.0f
                                                        else -> zoomSliderValue
                                                    }
                                                    cameraManager.switchCamera(lens.cameraId, lens.zoom)
                                                },
                                                label = { 
                                                    Text(
                                                        text = lens.label,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                                    ) 
                                                },
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.height(56.dp) // Botões mais altos para facilitar o toque
                                            )
                                        }
                                    }
                                }
                            }

                            ServerButton()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        checkPermissions()
        ipAddress = getLocalIpAddress()
        listCameras()
    }

    @Composable
    fun HeaderSection(ip: String, isRunning: Boolean) {
        val serverUrl = if (ip == "0.0.0.0") "SEM CONEXÃO WI-FI" else "http://$ip:8080"
        val qrCodeBitmap = remember(ip) { if (ip != "0.0.0.0") generateQRCode("http://$ip:8080") else null }
        var showQrDialog by remember { mutableStateOf(false) }

        if (showQrDialog) {
            AlertDialog(
                onDismissRequest = { showQrDialog = false },
                confirmButton = { 
                    Button(onClick = { showQrDialog = false }) { Text("FECHAR") } 
                },
                title = { Text("QR Code de Conexão", color = Color.White) },
                containerColor = Color.Black,
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        qrCodeBitmap?.let {
                            Surface(
                                modifier = Modifier.size(220.dp),
                                color = Color.White,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = BorderStroke(1.dp, Color(0xFF353535)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Endereço do servidor",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFBDBDBD)
                    )

                    Surface(
                        color = if (isRunning) Color(0xFF1F5E3B) else Color(0xFF4A3A10),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (isRunning) "ATIVO" else "PARADO",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = serverUrl,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (ip == "0.0.0.0") Color(0xFFFF6B6B) else Color(0xFFFFEB3B),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { copyToClipboard(serverUrl) }
                )

                Text(
                    text = "Toque no endereço para copiar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A9A9A)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { shareAddress(serverUrl) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEDEDED),
                            contentColor = Color.Black
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Button(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEDEDED),
                            contentColor = Color.Black
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    
                    IconButton(
                        onClick = { ipAddress = getLocalIpAddress() },
                        modifier = Modifier.size(48.dp).background(Color(0xFF2A2A2A), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
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

    private fun shareToWhatsApp(url: String) {
        // Mantido apenas por compatibilidade se necessário, mas shareAddress é preferível
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Controle a câmera do Campilot por aqui: $url")
            setPackage("com.whatsapp")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            shareAddress(url)
        }
    }

    @Composable
    fun ControlToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit) {
        // Não mais utilizado mas mantido para evitar erros de compilação se houver referências perdidas
    }

    @Composable
    fun ServerButton() {
        Button(
            onClick = { 
                if (isServiceRunning) CameraService.stop(this@MainActivity) 
                else CameraService.start(this@MainActivity)
                isServiceRunning = !isServiceRunning
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceRunning) Color(0xFFB3261E) else Color(0xFF9E7CFF),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.large
        ) { 
            Icon(
                imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isServiceRunning) "Parar transmissão" else "Iniciar servidor")
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
                        focal < 3.0f -> ".6x"
                        focal < 10.0f -> "1x"
                        focal < 20.0f -> "3x"
                        else -> "5x"
                    }
                    
                    // Armazenar ID mapeado para o slider
                    backCameraIds[label] = cameraId
                    
                    foundLenses.add(QuickLens(label, cameraId, 1.0f, focal))
                    if (label == "1x") foundLenses.add(QuickLens("2x", cameraId, 2.0f, focal + 0.1f))
                    if (label == "3x") {
                        // O S25 Ultra tem 3x e 5x ópticos. Vamos garantir que ambos apareçam.
                    }
                    if (label == "5x") {
                        foundLenses.add(QuickLens("10x", cameraId, 2.0f, focal + 0.1f))
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
