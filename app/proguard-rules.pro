# Add project specific ProGuard rules here.

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializable data classes
-keep,includedescriptorclasses class com.trueskies.android.data.remote.models.**$$serializer { *; }
-keepclassmembers class com.trueskies.android.data.remote.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.trueskies.android.data.remote.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep domain models
-keep,includedescriptorclasses class com.trueskies.android.domain.models.**$$serializer { *; }
-keepclassmembers class com.trueskies.android.domain.models.** {
    *** Companion;
}

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ZXing
-keep class com.google.zxing.** { *; }
