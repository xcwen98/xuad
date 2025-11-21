package com.xcw.xuad.ad

import android.app.Activity
import android.view.ViewGroup
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.log.XuLog
import com.xcw.xuad.network.AppApi
import com.xcw.xuad.network.entity.AppRecordRequest
import android.os.Build

/**
 * 开屏广告管理器
 * - 仅在 adEnabled=1 且 TTAdSdk 初始化成功后允许加载
 */
object SplashAdManager {

    private fun buildDeviceInfo(): String {
        val brand = Build.BRAND ?: ""
        val model = Build.MODEL ?: ""
        val os = "Android ${Build.VERSION.RELEASE ?: ""}"
        val sdk = "SDK ${Build.VERSION.SDK_INT}"
        val info = "brand=" + brand + "; model=" + model + "; os=" + os + "; sdk=" + sdk
        return info.take(1024)
    }

    private fun reportInteraction(interactionType: String, ecpm: Double?) {
        val req = AppRecordRequest(
            oaid = XuAdManager.getOaid() ?: "",
            ecpm = (ecpm ?: 0.0).coerceAtLeast(0.0),
            adType = "splash",
            interactionType = interactionType,
            deviceInfo = buildDeviceInfo()
        )
        kotlin.concurrent.thread(start=true) {
            runCatching { AppApi.record(req) }
                .onFailure { XuLog.e("record 接口调用失败: ${it}") }
        }
    }


    private fun screenSize(act: Activity, container: ViewGroup?): Pair<Int, Int> {
        val dm = act.resources.displayMetrics
        val w = container?.width ?: dm.widthPixels
        val h = container?.height ?: dm.heightPixels
        return Pair(if (w > 0) w else dm.widthPixels, if (h > 0) h else dm.heightPixels)
    }

    fun buildSplashAdslot(act: Activity, container: ViewGroup? = null): AdSlot? {
        val codeId = XuAdManager.adSplashId()
        if (codeId.isBlank()) {
            XuLog.e("开屏广告位ID为空，取消加载")
            return null
        }
        val (w, h) = screenSize(act, container)
        return AdSlot.Builder()
            .setCodeId(codeId)
            .setImageAcceptedSize(w, h)
            .setOrientation(TTAdConstant.VERTICAL)
            .setSupportDeepLink(true)
            .build()
    }

    fun loadSplashAd(
        act: Activity,
        container: ViewGroup?,
        timeoutMs: Int = 1500,
        onShow: (() -> Unit)? = null,
        onClosed: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (!XuAdManager.adEnabled()) {
            XuLog.w("广告开关关闭，跳过开屏加载")
            return
        }
        if (!TTAdManagerHolder.isInitialized()) {
            XuLog.w("TTAdSdk 未初始化成功，跳过开屏加载")
            return
        }

        val adSlot = buildSplashAdslot(act, container)
        if (adSlot == null) return

        val loader = TTAdSdk.getAdManager().createAdNative(act)
        loader.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(csjSplashAd: CSJSplashAd?) {
                XuLog.i("开屏广告加载成功")
            }

            override fun onSplashLoadFail(error: CSJAdError?) {
                XuLog.e("开屏广告加载失败: code=${error?.code} err=${error?.toString()}")
                onFailed?.invoke()
            }

            override fun onSplashRenderSuccess(csjSplashAd: CSJSplashAd?) {
                XuLog.i("开屏广告渲染成功，开始展示")
                showSplashAd(csjSplashAd, container, onShow, onClosed)
            }

            override fun onSplashRenderFail(ad: CSJSplashAd?, err: CSJAdError?) {
                XuLog.e("开屏广告渲染失败: code=${err?.code} err=${err?.toString()}")
                onFailed?.invoke()
            }
        }, timeoutMs)
    }

    /**
     * 预加载开屏广告（仅加载与渲染，不立即展示）
     * - 渲染成功后通过 onReady 返回可供展示的 CSJSplashAd
     */
    fun preloadSplashAd(
        act: Activity,
        container: ViewGroup?,
        timeoutMs: Int = 1500,
        onReady: (CSJSplashAd?) -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        if (!XuAdManager.adEnabled()) {
            XuLog.w("广告开关关闭，跳过开屏预加载")
            onFailed?.invoke()
            return
        }
        if (!TTAdManagerHolder.isInitialized()) {
            XuLog.w("TTAdSdk 未初始化成功，跳过开屏预加载")
            onFailed?.invoke()
            return
        }

        val adSlot = buildSplashAdslot(act, container)
        if (adSlot == null) { onFailed?.invoke(); return }

        val loader = TTAdSdk.getAdManager().createAdNative(act)
        loader.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(csjSplashAd: CSJSplashAd?) {
                XuLog.i("开屏广告预加载-加载成功")
            }

            override fun onSplashLoadFail(error: CSJAdError?) {
                XuLog.e("开屏广告预加载-加载失败: code=${error?.code} err=${error?.toString()}")
                onFailed?.invoke()
            }

            override fun onSplashRenderSuccess(csjSplashAd: CSJSplashAd?) {
                XuLog.i("开屏广告预加载-渲染成功，返回待展示对象")
                onReady(csjSplashAd)
            }

            override fun onSplashRenderFail(ad: CSJSplashAd?, err: CSJAdError?) {
                XuLog.e("开屏广告预加载-渲染失败: code=${err?.code} err=${err?.toString()}")
                onFailed?.invoke()
            }
        }, timeoutMs)
    }

    fun showSplashAd(ad: CSJSplashAd?, container: ViewGroup?, onShow: (() -> Unit)? = null, onClosed: (() -> Unit)? = null) {
        ad?.let { splash ->
            splash.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
                override fun onSplashAdShow(csjSplashAd: CSJSplashAd?) {
                    XuLog.i("开屏广告展示")
                    onShow?.invoke()
                    val manager = splash.mediationManager
                    val ecpmInfo = manager?.showEcpm
                    if (ecpmInfo != null) {
                        val ecpm = ecpmInfo.ecpm
                        val sdkName = ecpmInfo.sdkName
                        val slotId = ecpmInfo.slotId
                        XuLog.i("展示广告 eCPM=$ecpm sdk=$sdkName slot=$slotId")
                    }
                    val ecpmDouble = try { ecpmInfo?.ecpm?.toDoubleOrNull() } catch (_: Throwable) { null }
                    reportInteraction("view", ecpmDouble)
                }

                override fun onSplashAdClick(csjSplashAd: CSJSplashAd?) {
                    XuLog.i("开屏广告点击")
                    val manager = splash.mediationManager
                    val ecpmInfo = manager?.showEcpm
                    if (ecpmInfo != null) {
                        val ecpm = ecpmInfo.ecpm
                        val sdkName = ecpmInfo.sdkName
                        val slotId = ecpmInfo.slotId
                        XuLog.i("点击广告详情 eCPM=$ecpm sdk=$sdkName slot=$slotId")
                    }
                    val ecpmDouble = try { ecpmInfo?.ecpm?.toDoubleOrNull() } catch (_: Throwable) { null }
                    reportInteraction("click", ecpmDouble)
                }

                override fun onSplashAdClose(csjSplashAd: CSJSplashAd?, p1: Int) {
                    XuLog.i("开屏广告关闭")
                    onClosed?.invoke()
                }
            })
            if (container != null) {
                splash.showSplashView(container)
            } else {
                XuLog.w("开屏容器为空，无法展示")
            }
        }
    }
}