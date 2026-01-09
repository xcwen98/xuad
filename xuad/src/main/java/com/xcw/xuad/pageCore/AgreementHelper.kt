package com.xcw.xuad.pageCore

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户协议帮助类
 * 负责处理用户协议的显示、存储和回调逻辑
 * 采用单例模式管理协议状态
 */
object AgreementHelper {

    private const val PREFS_NAME = "user_agreement"
    private const val KEY_AGREEMENT_ACCEPTED = "agreement_accepted"

    // 协议弹窗显示状态
    private val _showAgreementDialog = MutableStateFlow(false)
    val showAgreementDialog: StateFlow<Boolean> = _showAgreementDialog.asStateFlow()

    /**
     * 检查并显示用户协议
     * @param activity 当前Activity
     * @param themeColor 主题色
     * @param appName 应用名称
     * @param onAgreementConfirmed 用户已同意/同意后的回调
     */
    fun checkAndShowAgreement(
        activity: ComponentActivity,
        themeColor: Color,
        appName: String,
        onAgreementConfirmed: () -> Unit
    ) {
        if (isAgreementAccepted(activity)) {
            // 已同意协议，直接执行回调
            onAgreementConfirmed()
        } else {
            // 未同意，显示协议弹窗
            _showAgreementDialog.value = true
        }
    }

    /**
     * 用户点击同意按钮
     * @param context 上下文
     * @param onAgreementConfirmed 同意后的回调
     */
    fun onAgreeClicked(context: Context, onAgreementConfirmed: () -> Unit) {
        saveAgreementAccepted(context)
        _showAgreementDialog.value = false
        onAgreementConfirmed()
    }

    /**
     * 用户点击不同意按钮
     * @param activity 当前Activity，用于退出应用
     */
    fun onDisagreeClicked(activity: ComponentActivity) {
        _showAgreementDialog.value = false
        // 退出应用
        activity.finish()
    }

    /**
     * 打开协议页面
     * @param context 上下文
     * @param protocolName 协议名称 ("用户协议" 或 "隐私协议")
     */
    fun openProtocol(
        context: Context,
        protocolName: String,
        webUrl: String,
        appName: String,
        webType: String
    ) {
        val htmlFile = when (protocolName) {
            "用户协议", "用户服务协议" -> "yhxy"
            "隐私协议", "隐私政策" -> "yszc"
            "信息收集清单" -> "xxsjqd"
            "第三方共享清单" -> "sdkgxqd"
            else -> return
        }
        openWebPage(context, htmlFile, protocolName, webUrl, appName, webType)
    }

    /**
     * 通用打开网页方法
     * @param context 上下文
     * @param htmlFile html文件名（不含后缀）
     * @param title 页面标题
     * @param webUrl 协议网页的基础URL
     * @param appName 应用名称
     * @param webType 网页类型参数
     */
    fun openWebPage(
        context: Context,
        htmlFile: String,
        title: String,
        webUrl: String,
        appName: String,
        webType: String
    ) {
        val url = "$webUrl/$htmlFile.html?name=$appName&type=$webType"
        WebDocumentAd.start(context, url, title)
    }

    /**
     * 检查用户是否已同意协议
     */
    private fun isAgreementAccepted(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_AGREEMENT_ACCEPTED, false)
    }

    /**
     * 保存用户同意协议的状态
     */
    private fun saveAgreementAccepted(context: Context) {
        getSharedPreferences(context).edit().putBoolean(KEY_AGREEMENT_ACCEPTED, true).apply()
    }

    /**
     * 获取SharedPreferences实例
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
