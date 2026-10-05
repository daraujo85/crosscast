# ProGuard rules for a Ktor, Coroutines, and Compose app.

# Keep - Application entry points
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}
-keep class com.campilot.** { *; }

# Keep - Kotlin specific
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class ** {
    @kotlin.coroutines.jvm.internal.DebugMetadata kotlin.coroutines.Continuation continuation;
}
-dontwarn kotlin.collections.List
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,InnerClasses,EnclosingMethod

# Keep - Ktor / Netty / CIO (and dependencies like coroutines, atomicfu, etc.)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.atomicfu.** { *; }
-dontwarn kotlinx.atomicfu.**
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Keep - Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }
-keepclassmembers class * implements androidx.compose.runtime.Composer {
  <methods>;
}
-keepclassmembers class * implements androidx.compose.runtime.Recomposer {
  <methods>;
}

# SLF4J
-dontwarn org.slf4j.impl.StaticLoggerBinder
