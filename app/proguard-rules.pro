# 1. Protect all Data Models (Keeps variable names exact for Firebase Sync & Backup)
-keep class com.fastbill.ahamed.model.** { *; }
-keep class com.fastbill.ahamed.database.** { *; }

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn retrofit2.KotlinExtensions
-keep,allowobfuscation interface <1>
# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-printmapping build/outputs/mapping/release/mapping.txt
-printconfiguration build/outputs/mapping/release/full-r8-config.txt
-keep public class * extends android.content.ContextWrapper {public *;}