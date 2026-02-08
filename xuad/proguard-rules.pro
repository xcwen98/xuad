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

# 强制保留所有 XUAD 代码，用于排查 Release 白屏问题
-keep class com.xcw.xuad.** { *; }

# ==============================================
# 第三方 SDK 混淆规则 (与 consumer-rules.pro 保持一致)
# ==============================================

# 友盟混淆配置开始
-keep class com.umeng.** {*;}
-keep class org.repackage.** {*;}
-keep class com.uyumao.** { *; }

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep public class com.xcw.xuad.R$*{
    public static final int *;
}
# 友盟补充
-dontwarn com.umeng.**
-dontwarn org.repackage.**

# OkHttp/Okio（网络）
-dontwarn okhttp3.**
-dontwarn okio.**

# Pangle/Bytedance 穿山甲聚合SDK
# 聚合混淆
-keep class bykvm*.** { *; }
-keep class com.bytedance.msdk.adapter.**{ public *; }
-keep class com.bytedance.msdk.api.** {
    public *;
}
-keep class com.bytedance.sdk.** { *; }
-keep class com.bykv.vk.** { *; }
-keep class com.pgl.ssdk.** { *; }
-keep class com.iab.omid.** { *; }
-dontwarn com.bytedance.sdk.**
-dontwarn com.bykv.vk.**
-dontwarn com.pgl.ssdk.**
-dontwarn com.iab.omid.**
