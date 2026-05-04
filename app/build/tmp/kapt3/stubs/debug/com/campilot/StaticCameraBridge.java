package com.campilot;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0012\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R(\u0010\u0003\u001a\u0010\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u0006\u0018\u00010\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/campilot/StaticCameraBridge;", "", "()V", "onFrame", "Lkotlin/Function1;", "", "", "getOnFrame", "()Lkotlin/jvm/functions/Function1;", "setOnFrame", "(Lkotlin/jvm/functions/Function1;)V", "app_debug"})
public final class StaticCameraBridge {
    @org.jetbrains.annotations.Nullable()
    private static kotlin.jvm.functions.Function1<? super byte[], kotlin.Unit> onFrame;
    @org.jetbrains.annotations.NotNull()
    public static final com.campilot.StaticCameraBridge INSTANCE = null;
    
    private StaticCameraBridge() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.jvm.functions.Function1<byte[], kotlin.Unit> getOnFrame() {
        return null;
    }
    
    public final void setOnFrame(@org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super byte[], kotlin.Unit> p0) {
    }
}