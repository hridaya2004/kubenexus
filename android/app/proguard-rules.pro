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

# kotlinx.serialization
# The library ships consumer rules, but the generated serializers are reached
# reflectively via a companion, so the DTOs are kept explicitly. Without this a
# minified release build fails to decode cluster responses at runtime while debug
# builds work, which is an expensive class of bug to discover on device.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class dev.hridaya.kubenexus.data.source.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class dev.hridaya.kubenexus.data.source.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep generic signatures for reflection
-keepattributes Signature
