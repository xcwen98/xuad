package com.xcw.xuad.ad

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.log.XuLog
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig

/**
 * 穿山甲聚合SDK管理器
 * - 仅在 userInit 成功后调用初始化
 * - 保证仅初始化一次
 * - 按照官方注意事项：主线程调用 init，回调在子线程
 */
object TTAdManagerHolder {
    @Volatile
    private var initialized = false

    fun isInitialized(): Boolean = initialized

    fun initMediationAdSdk(context: Context) {
        // 主线程调用
        if (Looper.getMainLooper() != Looper.myLooper()) {
            Handler(Looper.getMainLooper()).post { initInternal(context) }
        } else {
            initInternal(context)
        }
    }

    private fun initInternal(context: Context) {
        if (initialized) {
            XuLog.i("TTAdSdk 已初始化，跳过")
            return
        }

        val appId = XuAdManager.pangleAppId()
        if (appId.isBlank()) {
            XuLog.e("TTAdSdk 初始化失败：pangleAppId 为空，请在 init 接口返回后校验")
            return
        }

        val config = buildConfig(context, appId)
        try {
            TTAdSdk.init(context, config)
            TTAdSdk.start(object : TTAdSdk.Callback {
                override fun success() {
                    // 回调在子线程
                    XuLog.i("TTAdSdk 初始化成功")
                    initialized = true
                }

                override fun fail(code: Int, msg: String?) {
                    XuLog.e("TTAdSdk 初始化失败 code=$code msg=${msg ?: ""}")
                }
            })
        } catch (e: Throwable) {
            XuLog.e("TTAdSdk 初始化异常: ${e.message}")
        }
    }

    private fun buildConfig(context: Context, appId: String): TTAdConfig {
        val appName = appNameFromContext(context)
        return TTAdConfig.Builder()
            .appId(appId)
            .appName(appName)
            .useMediation(true) // 开启聚合功能（仅可设置一次）
            .debug(false) // 根据需要打开/关闭
            .themeStatus(0) // 0 正常；1 夜间
            .supportMultiProcess(false) // 默认不支持，如需多进程需同步配置各ADN
            .customController(getTTCustomController())
            .build()
    }

    private fun getTTCustomController(): TTCustomController {
        return object : TTCustomController() {
            override fun isCanUseLocation(): Boolean = true
            override fun isCanUsePhoneState(): Boolean = true
            override fun isCanUseWifiState(): Boolean = true
            override fun isCanUseWriteExternal(): Boolean = true
            override fun isCanUseAndroidId(): Boolean = true

            override fun getMediationPrivacyConfig(): MediationPrivacyConfig? {
                return object : MediationPrivacyConfig() {
                    override fun isLimitPersonalAds(): Boolean = false
                    override fun isProgrammaticRecommend(): Boolean = true
                }
            }
        }
    }

    private fun appNameFromContext(context: Context): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(context.packageName, 0)
            val label = ai.loadLabel(pm)
            label?.toString() ?: ""
        } catch (e: Exception) {
            // 兜底：读取应用标签失败时使用包名
            XuLog.w("获取应用名称失败：${e.message}")
            try { context.packageName } catch (_: Exception) { "" }
        }
    }
}