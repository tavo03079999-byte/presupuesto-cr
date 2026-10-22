# Proguard rules for Presupuesto Inteligente CR
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class net.sqlcipher.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.api.services.drive.** { *; }
