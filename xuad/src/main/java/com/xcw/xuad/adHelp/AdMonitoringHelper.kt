package com.xcw.xuad.adHelp

import android.content.Context
import com.xcw.xuad.ad.RealTimeMonitoring

/**
 * 广告监控辅助类
 * 封装广告相关的埋点和检测逻辑
 */
object AdMonitoringHelper {

    /**
     * 检查广告点击
     * 对应 RealTimeMonitoring.checkClickAd
     *
     * @param context 上下文
     * @param pageName 页面名称
     */
    fun checkClickAd(context: Context, pageName: String) {
        try {
            RealTimeMonitoring.checkClickAd(context, pageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
