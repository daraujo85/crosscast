package com.crosscast

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.Socket

class RtmpClient {

    companion object {
        private const val TAG = "RtmpClient"
        private const val CHUNK_SIZE = 4096
        private const val HANDSHAKE_SIZE = 1536
        
        private const val MSG_VIDEO = 9
        private const val MSG_AUDIO = 8
        private const val MSG_AMF0_CMD = 20
        
        private const val CMD_CONNECT = "connect"
        private const val CMD_CREATE_STREAM = "createStream"
        private const val CMD_PUBLISH = "publish"
    }

    enum class State {
        DISCONNECTED, HANDSHAKING, CONNECTING, STREAMING, ERROR
    }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state

    private var socket: Socket? = null
    private var outputStream: java.io.OutputStream? = null

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var transactionId = 1
    private var streamId = 0
    private val serverAddress = MutableStateFlow("")
    private var appName = ""

    private var onConnectSuccess: (() -> Unit)? = null
    private var onConnectFailure: ((Exception) -> Unit)? = null

    fun connect(url: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (_state.value != State.DISCONNECTED) {
            onFailure(IllegalStateException("Already connected or connecting"))
            return
        }

        onConnectSuccess = onSuccess
        onConnectFailure = onFailure

        clientScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    serverAddress.value = url
                    _state.value = State.HANDSHAKING
                }

                val parsed = parseRtmpUrl(url)
                appName = parsed.app

                socket = Socket(parsed.host, parsed.port)
                socket?.soTimeout = 10000
                outputStream = socket?.getOutputStream()

                val c0c1 = createHandshakeC0C1()
                outputStream?.write(c0c1)
                outputStream?.flush()

                val s0s1s2 = ByteArray(HANDSHAKE_SIZE * 2 + 1)
                val bytesRead = socket?.getInputStream()?.read(s0s1s2) ?: -1
                if (bytesRead < HANDSHAKE_SIZE * 2 + 1) {
                    withContext(Dispatchers.Main) {
                        _state.value = State.ERROR
                    }
                    withContext(Dispatchers.Main) {
                        onConnectFailure?.invoke(IllegalStateException("Handshake failed"))
                    }
                    return@launch
                }

                outputStream?.write(s0s1s2, 1, HANDSHAKE_SIZE)
                outputStream?.flush()

                withContext(Dispatchers.Main) {
                    _state.value = State.CONNECTING
                }
                sendConnect(appName)

                Log.d(TAG, "Connected to $url")
                withContext(Dispatchers.Main) {
                    onConnectSuccess?.invoke()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                withContext(Dispatchers.Main) {
                    _state.value = State.ERROR
                    onConnectFailure?.invoke(e)
                }
            }
        }
    }

    fun publish(streamKey: String): Boolean {
        if (_state.value != State.CONNECTING) return false
        sendCreateStream()
        sendPublish(streamKey)
        _state.value = State.STREAMING
        return true
    }

    fun sendVideoData(data: ByteArray, isKeyframe: Boolean) {
        if (_state.value != State.STREAMING) return
        try {
            val ts = (System.currentTimeMillis() and 0xFFFFFF).toInt()
            val videoData = createFlvVideoTag(data, isKeyframe)
            sendRtmpMessage(MSG_VIDEO, streamId, ts, videoData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send video", e)
        }
    }

    fun sendAudioData(data: ByteArray) {
        if (_state.value != State.STREAMING) return
        try {
            val ts = (System.currentTimeMillis() and 0xFFFFFF).toInt()
            val audioData = createFlvAudioTag(data)
            sendRtmpMessage(MSG_AUDIO, streamId, ts, audioData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send audio", e)
        }
    }

    fun disconnect() {
        try { socket?.close() } catch (e: Exception) { }
        socket = null
        outputStream = null
        _state.value = State.DISCONNECTED
    }

    private fun parseRtmpUrl(url: String): ParsedUrl {
        val withoutProtocol = url.removePrefix("rtmp://")
        val parts = withoutProtocol.split("/")
        val hostPort = parts[0].split(":")
        val host = hostPort[0]
        val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 1935 else 1935
        val app = if (parts.size > 1) parts[1] else "live"
        val streamKey = if (parts.size > 2) parts.drop(2).joinToString("/") else ""
        return ParsedUrl(host, port, app, streamKey)
    }

    private data class ParsedUrl(val host: String, val port: Int, val app: String, val streamKey: String)

    private fun createHandshakeC0C1(): ByteArray {
        val c0c1 = ByteArray(HANDSHAKE_SIZE + 1)
        c0c1[0] = 3
        val now = System.currentTimeMillis()
        c0c1[1] = ((now shr 24) and 0xFF).toByte()
        c0c1[2] = ((now shr 16) and 0xFF).toByte()
        c0c1[3] = ((now shr 8) and 0xFF).toByte()
        c0c1[4] = (now and 0xFF).toByte()
        java.security.SecureRandom().nextBytes(c0c1)
        return c0c1
    }

    private fun sendConnect(app: String) {
        val props = mapOf(
            "app" to app,
            "type" to "nonprivate",
            "flashVer" to "FMLE/3.0",
            "tcUrl" to serverAddress.value
        )
        val amf = buildAmfConnectCommand(app, props)
        sendRtmpMessage(MSG_AMF0_CMD, 0, 0, amf)
    }

    private fun sendCreateStream() {
        val amf = buildAmfCommand(CMD_CREATE_STREAM, transactionId++)
        sendRtmpMessage(MSG_AMF0_CMD, 0, 0, amf)
    }

    private fun sendPublish(streamKey: String) {
        val amf = buildAmfCommand(CMD_PUBLISH, transactionId++, listOf(streamKey, "live"))
        sendRtmpMessage(MSG_AMF0_CMD, streamId, 0, amf)
    }

    private fun buildAmfConnectCommand(app: String, props: Map<String, Any>): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.writeAmfString(CMD_CONNECT)
        baos.writeAmfNumber(transactionId++.toDouble())
        baos.write(0x03)
        for ((key, value) in props) {
            baos.writeAmfString(key)
            baos.writeAmfValue(value)
        }
        baos.write(0x00); baos.write(0x00); baos.write(0x09)
        return baos.toByteArray()
    }

    private fun buildAmfCommand(name: String, transId: Int, vararg args: Any?): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.writeAmfString(name)
        baos.writeAmfNumber(transId.toDouble())
        baos.write(0x05)
        for (arg in args) {
            when (arg) {
                is String -> baos.writeAmfString(arg)
                is Double -> baos.writeAmfNumber(arg)
                is Boolean -> baos.writeAmfBoolean(arg)
                else -> baos.write(0x05)
            }
        }
        return baos.toByteArray()
    }

    private fun sendRtmpMessage(type: Int, streamId: Int, timestamp: Int, data: ByteArray) {
        try {
            val out = outputStream ?: return
            
            var offset = 0
            var firstChunk = true
            
            while (offset < data.size) {
                if (firstChunk) {
                    val header = ByteArray(12)
                    header[0] = (type and 0x3F).toByte()
                    header[1] = ((timestamp shr 16) and 0xFF).toByte()
                    header[2] = ((timestamp shr 8) and 0xFF).toByte()
                    header[3] = (timestamp and 0xFF).toByte()
                    header[4] = ((data.size shr 16) and 0xFF).toByte()
                    header[5] = ((data.size shr 8) and 0xFF).toByte()
                    header[6] = (data.size and 0xFF).toByte()
                    header[7] = ((streamId shr 24) and 0xFF).toByte()
                    header[8] = ((streamId shr 16) and 0xFF).toByte()
                    header[9] = ((streamId shr 8) and 0xFF).toByte()
                    header[10] = (streamId and 0xFF).toByte()
                    out.write(header)
                    firstChunk = false
                } else {
                    val fmt3 = ((3 shl 6) or (type and 0x3F))
                    out.write(fmt3)
                }
                
                val chunkLen = minOf(CHUNK_SIZE - (if (firstChunk) 0 else 1), data.size - offset)
                out.write(data, offset, chunkLen)
                offset += chunkLen
            }
            
            out.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send RTMP message", e)
        }
    }

    private fun createFlvVideoTag(data: ByteArray, isKeyframe: Boolean): ByteArray {
        val frameType = if (isKeyframe) 0x17 else 0x27
        val baos = ByteArrayOutputStream()
        baos.write(frameType)
        baos.write(0x01)
        baos.write(0x00); baos.write(0x00); baos.write(0x00)
        baos.write(data)
        return baos.toByteArray()
    }

    private fun createFlvAudioTag(data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(0xAF)
        baos.write(0x01)
        baos.write(data)
        return baos.toByteArray()
    }

    private fun ByteArrayOutputStream.writeAmfString(s: String) {
        write(0x02)
        val bytes = s.toByteArray(Charsets.UTF_8)
        write((bytes.size shr 8) and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeAmfNumber(d: Double) {
        write(0x00)
        val bits = java.lang.Double.doubleToLongBits(d)
        for (i in 7 downTo 0) {
            write(((bits shr (i * 8)) and 0xFF).toInt())
        }
    }

    private fun ByteArrayOutputStream.writeAmfBoolean(b: Boolean) {
        write(0x01)
        write(if (b) 0x01 else 0x00)
    }

    private fun ByteArrayOutputStream.writeAmfValue(v: Any?) {
        when (v) {
            is String -> writeAmfString(v)
            is Number -> writeAmfNumber(v.toDouble())
            is Boolean -> writeAmfBoolean(v)
            else -> write(0x05)
        }
    }
}
