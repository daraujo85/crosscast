package com.crosscast

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RTMP Streamer using Android's MediaCodec for H.264 encoding.
 * Provides real-time streaming to RTMP servers like Restream.io.
 */
class RtmpStreamer(private val context: Context) {

    companion object {
        private const val TAG = "RtmpStreamer"
        const val DEFAULT_RTMP_URL = "rtmp://saopaulo.restream.io/live/re_3115918_event69ba8c5af63a497d8077e9713f66559b"
        
        // Encoding parameters
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_BITRATE = 2_500_000 // 2.5 Mbps
        private const val VIDEO_FRAMERATE = 30
        private const val I_FRAME_INTERVAL = 2 // Keyframe every 2 seconds
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val TIMEOUT_US = 10_000L
    }

    enum class StreamState {
        OFFLINE,
        ENCODING,
        CONNECTING,
        LIVE,
        ERROR
    }

    private val _streamState = MutableStateFlow(StreamState.OFFLINE)
    val streamState: StateFlow<StreamState> = _streamState

    private val _streamAddress = MutableStateFlow(DEFAULT_RTMP_URL)
    val streamAddress: StateFlow<String> = _streamAddress

    private val isStreaming = AtomicBoolean(false)
    private var encoder: MediaCodec? = null
    private var rtmpClient: RtmpClient? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var encoderThread: Thread? = null
    private var inputSurface: Surface? = null
    
    // SPS/PPS for H.264
    private var spsPpsData: ByteArray? = null
    private var firstFrameSent = false

    /**
     * Initialize the encoder and prepare for streaming.
     */
    fun initialize(): Surface? {
        if (encoder != null) {
            return inputSurface
        }

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAMERATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder?.createInputSurface()
            encoder?.start()
            
            Log.d(TAG, "Encoder initialized: ${VIDEO_WIDTH}x${VIDEO_HEIGHT}")
            return inputSurface
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encoder", e)
            _streamState.value = StreamState.ERROR
            return null
        }
    }

    /**
     * Start streaming to the specified RTMP URL.
     */
    fun startStream(address: String = DEFAULT_RTMP_URL) {
        if (isStreaming.getAndSet(true)) {
            Log.w(TAG, "Already streaming")
            return
        }

        _streamAddress.value = address
        _streamState.value = StreamState.CONNECTING

        if (encoder == null) {
            if (initialize() == null) {
                isStreaming.set(false)
                return
            }
        }

        encoderThread = Thread {
            processEncoderOutput()
        }.apply { start() }

        rtmpClient = RtmpClient()
        if (rtmpClient?.connect(address) == true) {
            rtmpClient?.publish("")
            _streamState.value = StreamState.LIVE
            Log.d(TAG, "Stream started: $address")
        } else {
            _streamState.value = StreamState.ERROR
            isStreaming.set(false)
        }
    }

    /**
     * Stop the current stream.
     */
    fun stopStream() {
        if (!isStreaming.getAndSet(false)) {
            return
        }

        encoderThread?.interrupt()
        encoderThread = null
        
        try {
            encoder?.stop()
            encoder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping encoder", e)
        }
        encoder = null
        inputSurface = null
        
        rtmpClient?.disconnect()
        rtmpClient = null
        
        firstFrameSent = false
        spsPpsData = null
        
        mainHandler.post {
            _streamState.value = StreamState.OFFLINE
        }
        Log.d(TAG, "Stream stopped")
    }

    /**
     * Check if currently streaming.
     */
    fun isStreaming(): Boolean = isStreaming.get()

    /**
     * Toggle streaming on/off.
     */
    fun toggleStream(address: String = DEFAULT_RTMP_URL) {
        if (isStreaming.get()) {
            stopStream()
        } else {
            startStream(address)
        }
    }

    /**
     * Process frames from the camera through the encoder.
     */
    fun onCameraFrame(imageProxy: ImageProxy) {
        if (!isStreaming.get()) {
            imageProxy.close()
            return
        }

        _streamState.value = StreamState.ENCODING

        try {
            val surface = inputSurface ?: run {
                imageProxy.close()
                return
            }

            val canvas = surface.lockHardwareCanvas()
            canvas.drawColor(android.graphics.Color.BLACK)
            
            val yBuffer = imageProxy.planes[0].buffer
            val yData = ByteArray(yBuffer.remaining())
            yBuffer.get(yData)
            
            val paint = android.graphics.Paint().apply {
                isAntiAlias = false
            }
            canvas.drawBitmap(
                yuvToBitmap(yData, imageProxy.width, imageProxy.height),
                0f, 0f, paint
            )
            
            surface.unlockCanvasAndPost(canvas)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        } finally {
            imageProxy.close()
            if (isStreaming.get()) {
                _streamState.value = StreamState.LIVE
            }
        }
    }

    /**
     * Encode a JPEG frame (for integration with existing MJPEG pipeline).
     */
    fun encodeFrame(jpegData: ByteArray, isKeyframe: Boolean = false) {
        if (!isStreaming.get()) return

        try {
            val surface = inputSurface ?: return

            val canvas = surface.lockHardwareCanvas()
            canvas.drawColor(android.graphics.Color.BLACK)
            
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            if (bitmap != null) {
                val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, VIDEO_WIDTH, VIDEO_HEIGHT, true)
                canvas.drawBitmap(scaled, 0f, 0f, null)
                if (scaled != bitmap) scaled.recycle()
                bitmap.recycle()
            }
            
            surface.unlockCanvasAndPost(canvas)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding frame", e)
        }
    }

    private fun processEncoderOutput() {
        val codec = encoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        val rtmp = rtmpClient

        while (isStreaming.get() && !Thread.currentThread().isInterrupted) {
            try {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Encoder format changed: ${codec.outputFormat}")
                        sendParameterSets()
                    }
                    outputIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val data = ByteArray(bufferInfo.size)
                            outputBuffer.get(data)
                            
                            val isKeyframe = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                            if (isKeyframe) {
                                sendParameterSets()
                            }
                            
                            sendNalUnits(data, isKeyframe)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
                
            } catch (e: Exception) {
                if (isStreaming.get()) {
                    Log.e(TAG, "Encoder output error", e)
                }
                break
            }
        }
    }

    private fun sendParameterSets() {
        val format = encoder?.outputFormat ?: return
        val csd = format.getByteBuffer("csd-0")?.let { buf ->
            val data = ByteArray(buf.remaining())
            buf.get(data)
            data
        }
        val csd1 = format.getByteBuffer("csd-1")?.let { buf ->
            val data = ByteArray(buf.remaining())
            buf.get(data)
            data
        }
        
        if (csd != null && csd1 != null) {
            spsPpsData = byteArrayOf(0x00, 0x00, 0x00, 0x01) + csd + byteArrayOf(0x00, 0x00, 0x00, 0x01) + csd1
        }
    }

    private fun sendNalUnits(data: ByteArray, isKeyframe: Boolean) {
        val rtmp = rtmpClient ?: return
        
        var offset = 0
        while (offset < data.size - 4) {
            if (data[offset] == 0x00.toByte() && data[offset + 1] == 0x00.toByte()) {
                val startCodeLen = if (data[offset + 2] == 0x00.toByte() && data[offset + 3] == 0x01.toByte()) 4 else if (data[offset + 2] == 0x01.toByte()) 3 else continue
                
                val nextOffset = findNextStartCode(data, offset + startCodeLen)
                val nalUnit = data.copyOfRange(offset + startCodeLen, nextOffset)
                
                val annexB = byteArrayOf(0x00, 0x00, 0x00, 0x01) + nalUnit
                rtmp.sendVideoData(annexB, isKeyframe)
                
                offset = nextOffset
            } else {
                offset++
            }
        }
    }

    private fun findNextStartCode(data: ByteArray, start: Int): Int {
        for (i in start until data.size - 3) {
            if (data[i] == 0x00.toByte() && data[i + 1] == 0x00.toByte()) {
                if (data[i + 2] == 0x01.toByte()) return i
                if (i + 3 < data.size && data[i + 2] == 0x00.toByte() && data[i + 3] == 0x01.toByte()) return i
            }
        }
        return data.size
    }

    private fun yuvToBitmap(yData: ByteArray, width: Int, height: Int): android.graphics.Bitmap {
        val argb = IntArray(width * height)
        for (i in 0 until width * height) {
            val y = yData[i].toInt() and 0xFF
            argb[i] = (0xFF000000.toInt() or (y shl 16) or (y shl 8) or y)
        }
        return android.graphics.Bitmap.createBitmap(argb, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }

    /**
     * Release resources.
     */
    fun release() {
        stopStream()
    }
}
