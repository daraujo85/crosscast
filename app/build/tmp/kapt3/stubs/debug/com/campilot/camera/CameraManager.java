package com.campilot.camera;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0012\n\u0002\u0010\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\t\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0019\u001a\u00020\u00142\u0006\u0010\u001a\u001a\u00020\u0010J\u001a\u0010\u001b\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\u000e\u0010 \u001a\u00020\u00142\u0006\u0010!\u001a\u00020\"J4\u0010#\u001a\u00020\u00142\u0006\u0010$\u001a\u00020\f2\u0006\u0010%\u001a\u00020\n2\u0006\u0010&\u001a\u00020\u000e2\n\b\u0002\u0010\'\u001a\u0004\u0018\u00010(2\b\b\u0002\u0010)\u001a\u00020\"J\u0018\u0010*\u001a\u00020\u00142\u0006\u0010\'\u001a\u00020(2\b\b\u0002\u0010+\u001a\u00020\"J\u0016\u0010,\u001a\u00020\u00142\u0006\u0010-\u001a\u00020\"2\u0006\u0010.\u001a\u00020\"J\u0006\u0010/\u001a\u00020\u0010J\u0010\u00100\u001a\u00020\u00132\u0006\u0010\u001c\u001a\u00020\u001dH\u0002R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R(\u0010\u0011\u001a\u0010\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u0014\u0018\u00010\u0012X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u0016\"\u0004\b\u0017\u0010\u0018\u00a8\u00061"}, d2 = {"Lcom/campilot/camera/CameraManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "activeCamera", "Landroidx/camera/core/Camera;", "cameraProvider", "Landroidx/camera/lifecycle/ProcessCameraProvider;", "currentExecutor", "Ljava/util/concurrent/Executor;", "currentLifecycleOwner", "Landroidx/lifecycle/LifecycleOwner;", "currentPreviewView", "Landroidx/camera/view/PreviewView;", "isFlashEnabled", "", "onFrameCaptured", "Lkotlin/Function1;", "", "", "getOnFrameCaptured", "()Lkotlin/jvm/functions/Function1;", "setOnFrameCaptured", "(Lkotlin/jvm/functions/Function1;)V", "enableFlash", "enabled", "imageProxyToJpegWithRotation", "image", "Landroidx/camera/core/ImageProxy;", "rotationDegrees", "", "setZoom", "ratio", "", "startCameraWithPreview", "lifecycleOwner", "executor", "previewView", "cameraId", "", "zoomRatio", "switchCamera", "zoom", "tapToFocus", "x", "y", "toggleFlash", "yuv420888ToNv21", "app_debug"})
public final class CameraManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.core.Camera activeCamera;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.lifecycle.ProcessCameraProvider cameraProvider;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.view.PreviewView currentPreviewView;
    @org.jetbrains.annotations.Nullable()
    private androidx.lifecycle.LifecycleOwner currentLifecycleOwner;
    @org.jetbrains.annotations.Nullable()
    private java.util.concurrent.Executor currentExecutor;
    private boolean isFlashEnabled = false;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function1<? super byte[], kotlin.Unit> onFrameCaptured;
    
    public CameraManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.jvm.functions.Function1<byte[], kotlin.Unit> getOnFrameCaptured() {
        return null;
    }
    
    public final void setOnFrameCaptured(@org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super byte[], kotlin.Unit> p0) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.camera.camera2.interop.ExperimentalCamera2Interop.class})
    public final void startCameraWithPreview(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.LifecycleOwner lifecycleOwner, @org.jetbrains.annotations.NotNull()
    java.util.concurrent.Executor executor, @org.jetbrains.annotations.NotNull()
    androidx.camera.view.PreviewView previewView, @org.jetbrains.annotations.Nullable()
    java.lang.String cameraId, float zoomRatio) {
    }
    
    public final void enableFlash(boolean enabled) {
    }
    
    public final boolean toggleFlash() {
        return false;
    }
    
    public final void tapToFocus(float x, float y) {
    }
    
    public final void setZoom(float ratio) {
    }
    
    public final void switchCamera(@org.jetbrains.annotations.NotNull()
    java.lang.String cameraId, float zoom) {
    }
    
    private final byte[] imageProxyToJpegWithRotation(androidx.camera.core.ImageProxy image, int rotationDegrees) {
        return null;
    }
    
    private final byte[] yuv420888ToNv21(androidx.camera.core.ImageProxy image) {
        return null;
    }
}