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
# 友盟补充（已有keep，补充不报警）
-dontwarn com.umeng.**
-dontwarn org.repackage.**
# 友盟混淆配置结束

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

-keep class com.xcw.xuad.XuAdManager { *; }
-keep class com.xcw.xuad.ad.RealTimeMonitoring { *; }
-keep class com.xcw.xuad.utils.WeatherUtils { *; }
-keep class com.xcw.xuad.log.XuLog { *; }
-keep class com.xcw.xuad.ad.BannerAdManager { *; }
