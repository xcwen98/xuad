package com.xcw.xuad.ad

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdDislike
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.log.XuLog
import com.xcw.xuad.network.ApiService
import com.xcw.xuad.network.entity.AppRecordRequest
import kotlin.collections.isNotEmpty

/**
 * Banner 广告管理器
 */
object BannerAdManager {

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
            adType = "banner",
            interactionType = interactionType,
            deviceInfo = buildDeviceInfo()
        )
        kotlin.concurrent.thread(start = true) {
            runCatching { ApiService.record(req) }
                .onFailure { XuLog.e("record 接口调用失败: ${it}") }
        }
    }

    private fun screenWidthDp(act: Activity): Float {
        val dm = act.resources.displayMetrics
        return dm.widthPixels / dm.density
    }

    fun buildBannerAdslot(act: Activity): AdSlot? {
        val codeId = XuAdManager.adBannerId()
        if (codeId.isBlank()) {
            XuLog.e("Banner 广告位ID为空，取消加载")
            return null
        }
        val widthDp = screenWidthDp(act)
        return AdSlot.Builder()
            .setCodeId(codeId)
            .setExpressViewAcceptedSize(widthDp, 200f)
            .setSupportDeepLink(true)
            .build()
    }

    fun loadBannerAd(act: Activity, container: ViewGroup?, onLoadComplete: ((Boolean) -> Unit)? = null) {
        XuLog.d("BannerAdManager: loadBannerAd called")
        
        if (!XuAdManager.adEnabled()) {
            XuLog.w("广告开关关闭，跳过 Banner 加载")
            onLoadComplete?.invoke(false)
            return
        }
        XuLog.d("BannerAdManager: 广告开关已开启")
        
        if (!TTAdManagerHolder.isInitialized()) {
            XuLog.w("TTAdSdk 未初始化成功，跳过 Banner 加载")
            onLoadComplete?.invoke(false)
            return
        }
        XuLog.d("BannerAdManager: TTAdSdk 已初始化")

        val adSlot = buildBannerAdslot(act)
        if (adSlot == null) {
            XuLog.w("BannerAdManager: adSlot 为空，无法加载广告")
            onLoadComplete?.invoke(false)
            return
        }
        XuLog.d("BannerAdManager: adSlot 创建成功，开始加载广告")

        val adNativeLoader: TTAdNative = TTAdSdk.getAdManager().createAdNative(act)
        adNativeLoader.loadBannerExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onNativeExpressAdLoad(ads: MutableList<TTNativeExpressAd>?) {
                XuLog.i("Banner 广告加载成功，count=${ads?.size ?: 0}")
                ads?.let {
                    if (it.isNotEmpty()) {
                        val ad: TTNativeExpressAd = it[0]
                        showBannerView(act, ad, container)
                        onLoadComplete?.invoke(true)
                    } else {
                        onLoadComplete?.invoke(false)
                    }
                } ?: onLoadComplete?.invoke(false)
            }

            override fun onError(code: Int, message: String?) {
                XuLog.e("Banner 广告加载失败: code=${code} err=${message}")
                onLoadComplete?.invoke(false)
            }
        })
    }

    fun showBannerView(act: Activity, bannerAd: TTNativeExpressAd?, container: ViewGroup?) {
        bannerAd?.setExpressInteractionListener(object : TTNativeExpressAd.ExpressAdInteractionListener {
            override fun onAdClicked(view: View?, type: Int) {
                XuLog.i("Banner 广告点击")
                val manager = bannerAd.mediationManager
                val ecpmInfo = manager?.showEcpm
                val ecpmDouble = try { ecpmInfo?.ecpm?.toDoubleOrNull() } catch (_: Throwable) { null }
                reportInteraction("click", ecpmDouble)
            }

            override fun onAdShow(view: View?, type: Int) {
                XuLog.i("Banner 广告展示")
                val manager = bannerAd.mediationManager
                val ecpmInfo = manager?.showEcpm
                if (ecpmInfo != null) {
                    XuLog.i("展示广告 eCPM=${ecpmInfo.ecpm} sdk=${ecpmInfo.sdkName} slot=${ecpmInfo.slotId}")
                }
                val ecpmDouble = try { ecpmInfo?.ecpm?.toDoubleOrNull() } catch (_: Throwable) { null }
                reportInteraction("view", ecpmDouble)
            }

            override fun onRenderFail(view: View?, msg: String?, code: Int) {
                XuLog.e("Banner 广告渲染失败: code=${code} msg=${msg}")
            }

            override fun onRenderSuccess(view: View?, width: Float, height: Float) {
                XuLog.i("Banner 广告渲染成功: width=${width} height=${height}")
            }
        })

        bannerAd?.setDislikeCallback(act, object : TTAdDislike.DislikeInteractionCallback {
            override fun onShow() {}
            override fun onSelected(position: Int, value: String?, enforce: Boolean) {
                // 用户点击了 dislike 按钮
                container?.removeAllViews()
            }
            override fun onCancel() {}
        })

        val bannerView: View? = bannerAd?.expressAdView
        if (bannerView != null) {
            container?.removeAllViews()
            container?.addView(bannerView)
            bannerAd?.render()
        }
    }
}