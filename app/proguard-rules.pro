# KubeNexus Proguard / R8 Configuration

# Go Mobile JNI Bridge
-keep class go.** { *; }
-keep class client.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep generic signatures for reflection
-keepattributes Signature
