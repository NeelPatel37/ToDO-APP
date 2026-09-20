# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase Realtime Database rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod
-keep class com.google.firebase.** { *; }

# Keep your model classes from being obfuscated (Required for Firebase mapping)
# This ensures that Firebase can map data from the database to your data classes
-keep class com.neelpatel.todo.tasktracker.model.** { *; }
-keepclassmembers class com.neelpatel.todo.tasktracker.model.** {
    <fields>;
    <methods>;
}

# Lottie rules
-keep class com.airbnb.lottie.** { *; }

# AndroidX and Compose rules
-keep class androidx.compose.** { *; }

# Keep line numbers and source file names for better stack traces in release crash reports
-keepattributes SourceFile,LineNumberTable

# Remove logging for release builds to improve performance and security
# This works in conjunction with proguard-android-optimize.txt
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Ignore warnings that may occur during the build process
-dontwarn com.google.firebase.**
-dontwarn androidx.compose.**
