package com.xcw.xuad.pageCore

import android.content.Context
import com.xcw.xuad.network.ApiService
import com.xcw.xuad.network.entity.AppFeedbackRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.runCatching
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.text.trim

/**
 * 反馈功能管理类
 * 处理反馈相关的业务逻辑
 */
object FeedbackManager {

    /**
     * 提交反馈
     *
     * @param context 上下文
     * @param subject 反馈主题
     * @param content 反馈内容
     * @param contact 联系方式 (可选)
     * @return 提交结果 (Result)
     */
    suspend fun submitFeedback(
        context: Context,
        subject: String,
        content: String,
        contact: String?,
        packageName: String? = null,
        channelName: String? = null,
        version: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val finalPackageName = packageName?.takeIf { it.isNotBlank() }
                    ?: context.packageName
                    ?: "com.leihe.lhkelong1"

                val finalChannelName = channelName?.takeIf { it.isNotBlank() }
                    ?: "xiaomi"

                val finalVersion = version?.takeIf { it.isNotBlank() }
                    ?: runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                    ?: "1.1"

                val feedbackRequest = AppFeedbackRequest(
                    packageName = finalPackageName,
                    channelName = finalChannelName,
                    version = finalVersion,
                    subject = subject.trim(),
                    content = content.trim(),
                    contact = contact?.trim()?.ifBlank { null }
                )
                ApiService.feedback(feedbackRequest)
                Unit
            }
        }
    }

    /**
     * 提交反馈（封装输入校验与生命周期回调）
     *
     * @param context 上下文
     * @param scope 组合的协程作用域
     * @param subject 主题
     * @param content 内容
     * @param contact 联系方式
     * @param onValidationError 输入校验失败回调 (subjectError, contentError)
     * @param onStart 提交开始回调
     * @param onSuccess 提交成功回调（保持现有需求：无论结果均提示成功由调用方控制）
     * @param onComplete 提交结束回调（用于恢复 UI 状态）
     * @param packageName 包名（可选，不传则尝试从Context获取，失败则使用默认值）
     * @param channelName 渠道名（可选，不传则使用默认值）
     * @param version 版本号（可选，不传则尝试从Context获取，失败则使用默认值）
     */
    fun submit(
        context: Context,
        scope: CoroutineScope,
        subject: String,
        content: String,
        contact: String?,
        onValidationError: (Boolean, Boolean) -> Unit,
        onStart: () -> Unit,
        onSuccess: () -> Unit,
        onComplete: () -> Unit,
        packageName: String? = null,
        channelName: String? = null,
        version: String? = null
    ) {
        val subjectError = subject.isBlank()
        val contentError = content.isBlank()
        if (subjectError || contentError) {
            onValidationError(subjectError, contentError)
            return
        }

        scope.launch {
            onStart()
            submitFeedback(context, subject, content, contact, packageName, channelName, version)
            onSuccess()
            onComplete()
        }
    }
}
