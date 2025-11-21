package com.xcw.xuad.network

import com.xcw.xuad.log.XuLog
import com.xcw.xuad.network.entity.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ApiService {
    private val client = NetworkConfig.client
    private val json = NetworkConfig.json
    private const val CONTENT_TYPE = "application/json; charset=utf-8"

    private fun postJson(path: String, jsonBody: String): String {
        val url = NetworkConfig.baseUrl + path
        
        // 打印请求参数
        XuLog.d("=== API Request ===")
        XuLog.d("URL: $url")
        XuLog.d("Request JSON: $jsonBody")
        
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(CONTENT_TYPE.toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                val responseBody = resp.body?.string() ?: ""
                // 检查HTTP状态码
                if (!resp.isSuccessful) {
                    XuLog.e("HTTP请求失败: ${resp.code} ${resp.message}")
                    throw Exception("HTTP请求失败: ${resp.code} ${resp.message}")
                }
                
                return responseBody
            }
        } catch (e: Exception) {
            XuLog.e("网络请求异常: ${e.message}")
            XuLog.e("异常详情: ${e}")
            throw e
        }
    }

    fun init(req: AppInitRequest): ApiResult<AppInitResponse> {
        XuLog.d("调用 init 接口")
        val body = json.encodeToString(req)
        val respStr = postJson("/app/init", body)
        val result = json.decodeFromString(ApiResult.serializer(AppInitResponse.serializer()), respStr)
        XuLog.d("init 接口调用完成")
        return result
    }

    fun record(req: AppRecordRequest): ApiLongResult {
        XuLog.d("调用 record 接口")
        val normalized = req.copy(
            oaid = req.oaid.trim(),
            adType = req.adType.trim().lowercase(),
            interactionType = req.interactionType.trim().lowercase(),
            deviceInfo = req.deviceInfo.replace("\n", " ").take(1024)
        )
        val body = json.encodeToString(normalized)
        val respStr = postJson("/app/record", body)
        val result = json.decodeFromString(ApiLongResult.serializer(), respStr)
        XuLog.d("record 接口调用完成")
        return result
    }

    fun userInit(req: AppUserInitRequest): ApiLongResult {
        XuLog.d("调用 userInit 接口")
        val normalized = req.copy(
            channelName = req.channelName.trim().lowercase()
        )
        val body = json.encodeToString(normalized)
        val respStr = postJson("/app/user-init", body)
        val result = json.decodeFromString(ApiLongResult.serializer(), respStr)
        XuLog.d("userInit 接口调用完成")
        return result
    }

    fun feedback(req: AppFeedbackRequest): ApiStringResult {
        XuLog.d("调用 feedback 接口")
        val body = json.encodeToString(req)
        val respStr = postJson("/app/feedback", body)
        val result = json.decodeFromString(ApiStringResult.serializer(), respStr)
        XuLog.d("feedback 接口调用完成")
        return result
    }

    /**
     * 广告策略校验
     * 调用后端 `/app/ad-strategy/check` 接口，根据渠道与近期点击情况返回精准广告策略字符串。
     * - 统一入参：`AppUserInitRequest`
     * - 规范化处理：`channelName` 转小写、`oaid` 去空白、`deviceInfo` 去换行并截断 ≤1024 字符
     * @param req 用户初始化参数（包含包名、渠道、版本、oaid、设备信息）
     * @return `ApiStringResult`，`data` 字段为策略字符串（如 `strict`），或为空字符串
     */
    fun checkAdStrategy(req: AppUserInitRequest): ApiStringResult {
        XuLog.d("调用 ad-strategy/check 接口")
        val normalized = req.copy(
            channelName = req.channelName.trim().lowercase(),
            oaid = req.oaid.trim(),
            deviceInfo = req.deviceInfo.replace("\n", " ").take(1024)
        )
        val body = json.encodeToString(normalized)
        val respStr = postJson("/app/ad-strategy/check", body)
        val result = json.decodeFromString(ApiStringResult.serializer(), respStr)
        XuLog.d("ad-strategy/check 接口调用完成")
        return result
    }
}