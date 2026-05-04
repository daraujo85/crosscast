package com.campilot.camera;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fR\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0010"}, d2 = {"Lcom/campilot/camera/CameraControlRegistry;", "", "()V", "_commands", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/campilot/camera/CameraCommand;", "commands", "Lkotlinx/coroutines/flow/SharedFlow;", "getCommands", "()Lkotlinx/coroutines/flow/SharedFlow;", "emitCommand", "", "cameraId", "", "zoom", "", "app_debug"})
public final class CameraControlRegistry {
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.MutableSharedFlow<com.campilot.camera.CameraCommand> _commands = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.SharedFlow<com.campilot.camera.CameraCommand> commands = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.campilot.camera.CameraControlRegistry INSTANCE = null;
    
    private CameraControlRegistry() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.campilot.camera.CameraCommand> getCommands() {
        return null;
    }
    
    public final void emitCommand(@org.jetbrains.annotations.NotNull()
    java.lang.String cameraId, float zoom) {
    }
}