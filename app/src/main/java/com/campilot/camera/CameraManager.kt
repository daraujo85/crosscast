package com.campilot.camera

import android.content.Context
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import android.graphics.*
import android.view.Surface
import java.io.ByteArrayOutputStream

class CameraManager(private val context: Context) {

    private var activeCamera: androidx.camera.core.Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentPreviewView: PreviewView? = null
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentExecutor: Executor? = null
    private var isFlashEnabled: Boolean = false
    
    var onFrameCaptured: ((ByteArray) -> Unit)? = null

    @OptIn(ExperimentalCamera2Interop::class)
    fun startCameraWithPreview(
        lifecycleOwner: LifecycleOwner,
        executor: Executor,
        previewView: PreviewView,
        cameraId: String? = null,
        zoomRatio: Float = 1.0f
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentExecutor = executor
        currentPreviewView = previewView

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val cameraSelector = if (cameraId != null) {
                    CameraSelector.Builder()
                        .addCameraFilter { cameraInfos ->
                            cameraInfos.filter { info ->
                                Camera2CameraInfo.from(info).cameraId == cameraId
                            }
                        }
                        .build()
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val jpegBytes = imageProxyToJpegWithRotation(imageProxy, rotation)
                    if (jpegBytes != null) {
                        onFrameCaptured?.invoke(jpegBytes)
                    }
                    imageProxy.close()
                }

                cameraProvider?.unbindAll()
                activeCamera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
                setZoom(zoomRatio)
                enableFlash(isFlashEnabled)
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to start camera", e)
            }
        }, executor)
    }

    fun enableFlash(enabled: Boolean) {
        isFlashEnabled = enabled
        activeCamera?.let {
            Log.d("CameraManager", "Setting torch to: $enabled")
            it.cameraControl.enableTorch(enabled)
        } ?: Log.e("CameraManager", "Cannot enable flash: activeCamera is null")
    }

    fun toggleFlash(): Boolean {
        isFlashEnabled = !isFlashEnabled
        activeCamera?.let {
            Log.d("CameraManager", "Toggling torch to: $isFlashEnabled")
            it.cameraControl.enableTorch(isFlashEnabled)
        } ?: Log.e("CameraManager", "Cannot toggle flash: activeCamera is null")
        return isFlashEnabled
    }

    fun tapToFocus(x: Float, y: Float) {
        val factory = currentPreviewView?.meteringPointFactory ?: return
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        activeCamera?.cameraControl?.startFocusAndMetering(action)
    }

    fun setZoom(ratio: Float) {
        activeCamera?.cameraControl?.setZoomRatio(ratio)
    }

    fun switchCamera(cameraId: String, zoom: Float = 1.0f) {
        val owner = currentLifecycleOwner ?: return
        val exec = currentExecutor ?: return
        val view = currentPreviewView ?: return
        startCameraWithPreview(owner, exec, view, cameraId, zoom)
    }

    private fun imageProxyToJpegWithRotation(image: ImageProxy, rotationDegrees: Int): ByteArray? {
        val nv21 = yuv420888ToNv21(image)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 80, out)
        
        val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        val finalOut = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, finalOut)
        return finalOut.toByteArray()
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        var outputOffset = 0

        val yBuffer = yPlane.buffer.duplicate()
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val yRow = ByteArray(yRowStride)
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(yRow, 0, minOf(yRowStride, yBuffer.remaining()))
            var col = 0
            while (col < width) {
                nv21[outputOffset++] = yRow[col * yPixelStride]
                col++
            }
        }

        val uvHeight = height / 2
        val uvWidth = width / 2
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        val uRow = ByteArray(uRowStride)
        val vRow = ByteArray(vRowStride)

        for (row in 0 until uvHeight) {
            uBuffer.position(row * uRowStride)
            vBuffer.position(row * vRowStride)
            uBuffer.get(uRow, 0, minOf(uRowStride, uBuffer.remaining()))
            vBuffer.get(vRow, 0, minOf(vRowStride, vBuffer.remaining()))

            var col = 0
            while (col < uvWidth) {
                val uIndex = col * uPixelStride
                val vIndex = col * vPixelStride
                nv21[outputOffset++] = vRow[vIndex]
                nv21[outputOffset++] = uRow[uIndex]
                col++
            }
        }

        return nv21
    }
}
