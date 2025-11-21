package com.xcw.xuad.network

import com.xcw.xuad.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.Duration

object NetworkConfig {
    // 基础地址来自 BuildConfig 字段 APIURL
    val baseUrl: String = BuildConfig.APIURL.trimEnd('/')

    // OkHttp 客户端配置
    val client: OkHttpClient = OkHttpClient.Builder()
        // 统一超时：3秒
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .writeTimeout(Duration.ofSeconds(3))
        // 整体调用超时：3秒，确保“不超过3秒”原则
        .callTimeout(Duration.ofSeconds(3))
        .build()

    // Kotlinx Serialization JSON 配置
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = false
    }
}