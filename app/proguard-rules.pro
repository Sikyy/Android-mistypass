# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep,allowobfuscation interface * { @retrofit2.http.* <methods>; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.mistyislet.app.domain.model.** { *; }
-keepclassmembers class com.mistyislet.app.data.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Nordic BLE
-keep class no.nordicsemi.android.ble.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# Glance Widget
-keep class com.mistyislet.app.widget.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Security - strip logs in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
