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

# Google Play Services Auth (Passkey 지원)
-keep class com.google.android.gms.auth.api.identity.** { *; }
-keep class com.google.android.gms.fido.fido2.api.common.** { *; }
-dontwarn com.google.android.gms.auth.api.identity.**
-dontwarn com.google.android.gms.fido.fido2.api.common.**

# Credential Manager
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**