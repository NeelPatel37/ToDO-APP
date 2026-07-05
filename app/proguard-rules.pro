# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase Realtime Database rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }

# Keep your model classes from being obfuscated (Required for Firebase mapping)
-keep class com.example.to_dotasktracker.model.** { *; }

# Lottie rules
-keep class com.airbnb.lottie.** { *; }

# AndroidX and Compose rules
-keep class androidx.compose.** { *; }

# Remove logging for release builds (Optional but recommended)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}