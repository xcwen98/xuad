package com.xcw.xuad.ad

import android.app.Activity
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.log.XuLog
import com.xcw.xuad.network.ApiService
import com.xcw.xuad.network.entity.AppRecordRequest
import android.os.Build

/**
 * 插屏（全屏视频）广告管理器
 */
object InterstitialAdManager {

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
            adType = "interstitial",
            interactionType = interactionType,
            deviceInfo = buildDeviceInfo()
        )
        kotlin.concurrent.thread(start = true) {
            runCatching { ApiService.record(req) }
                .onFailure { XuLog.e("record 接口调用失败: ${it}") }
        }
    }

    fun buildInterstitialFullAdslot(act: Activity): AdSlot? {
        val codeId = XuAdManager.adInterstitialId()
        if (codeId.isBlank()) {
            XuLog.e("插屏广告位ID为空，取消加载")
            return null
        }
        return AdSlot.Builder()
            .setCodeId(codeId)
            .setOrientation(TTAdConstant.VERTICAL)
            .setSupportDeepLink(true)
            .build()
    }

    fun loadInterstitialFullAd(
        act: Activity,
        onShow: (() -> Unit)? = null,
        onClosed: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (!XuAdManager.adEnabled()) {
            XuLog.w("广告开关关闭，跳过插屏加载")
            return
        }
        if (!TTAdManagerHolder.isInitialized()) {
            XuLog.w("TTAdSdk 未初始化成功，跳过插屏加载")
            return
        }

        // 显示加载蒙版
        LoadingOverlay.show(act)

        val adSlot = buildInterstitialFullAdslot(act)
        if (adSlot == null) { LoadingOverlay.dismiss(); return }

        val loader = TTAdSdk.getAdManager().createAdNative(act)
        loader.loadFullScreenVideoAd(adSlot, object : TTAdNative.FullScreenVideoAdListener {
            override fun onError(code: Int, message: String?) {
                XuLog.e("插屏广告加载失败: code=${code} err=${message}")
                LoadingOverlay.dismiss()
                onFailed?.invoke()
            }

            override fun onFullScreenVideoAdLoad(ad: TTFullScreenVideoAd?) {
                XuLog.i("插屏广告加载成功")
            }

            override fun onFullScreenVideoCached() {
                // 已废弃的回调，不使用
            }

            override fun onFullScreenVideoCached(ad: TTFullScreenVideoAd?) {
                XuLog.i("插屏广告缓存完成，准备展示")
                showInterstitialFullAd(act, ad, onShow, onClosed)
            }
        })
    }

    fun showInterstitialFullAd(
        act: Activity,
        ad: TTFullScreenVideoAd?,
        onShow: (() -> Unit)? = null,
        onClosed: (() -> Unit)? = null
    ) {
        // 立即关闭加载蒙版
        LoadingOverlay.dismiss()

        ad?.let { fsAd ->
            if (fsAd.mediationManager?.isReady == true) {
                fsAd.setFullScreenVideoAdInteractionListener(object : TTFullScreenVideoAd.FullScreenVideoAdInteractionListener {
                    override fun onAdShow() {
                        XuLog.i("插屏广告展示")
                        onShow?.invoke()
                        val manager = fsAd.mediationManager
                        val ecpmInfo = manager?.showEcpm
                        if (ecpmInfo != null) {
                            XuLog.i("展示广告 eCPM=${ecpmInfo.ecpm} sdk=${ecpmInfo.sdkName} slot=${ecpmInfo.slotId}")
                        }
                        val ecpmDouble = try { ecpmInfo?.ecpm?.toDoubleOrNull() } catch (_: Throwable) { null }
                        reportInteraction("view", ecpmDouble)
                    }

                    override fun onAdVideoBarClick() {
                        XuLog.i("插屏广告点击")
                        val manager = fsAd.mediationManager
                        val ecpmInfo = manager?.showEcpm
                        if (ecpmInfo != null) {
                            XuLog.i("点击广告详情 eCPM=${ecpmInfo.ecpm} sdk=${ecpmInfo.sdkName} slot=${ecpmInfo.slotId}")
                        }
                        val ecpmDouble = try { ecpmInfo?.ecpm?.toDoubleOrNull() } catch (_: Throwable) { null }
                        reportInteraction("click", ecpmDouble)
                    }

                    override fun onAdClose() {
                        XuLog.i("插屏广告关闭")
                        onClosed?.invoke()
                    }

                    override fun onVideoComplete() {
                        XuLog.i("插屏视频播放完成")
                    }

                    override fun onSkippedVideo() {
                        XuLog.i("插屏视频跳过")
                    }
                })
                fsAd.showFullScreenVideoAd(act)
            } else {
                XuLog.w("TTFullScreenVideoAd 未就绪，无法展示")
            }
        }
    }
}