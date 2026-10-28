# Proguard rules for PresupuestoCR
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class net.sqlcipher.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
