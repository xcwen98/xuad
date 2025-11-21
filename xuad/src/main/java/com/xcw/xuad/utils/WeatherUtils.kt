package com.xcw.xuad.utils

import com.xcw.xuad.XuAdManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 天气工具类
 * 提供获取天气信息的功能
 */
object WeatherUtils {

    /**
     * 获取天气信息（异步方法）
     * 调用指定的API接口获取天气信息
     * @return 天气信息的完整JSON字符串，获取失败返回空字符串
     */
    suspend fun getWeatherInfo(): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl =
                    "https://cn.apihz.cn/api/tianqi/tqybip.php?id=${XuAdManager.apihzId()}&key=${XuAdManager.apihzKey()}"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    // 统一超时：3秒
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", "Android-App")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val jsonResponse = reader.readText()
                    reader.close()
                    connection.disconnect()

                    // 直接返回完整的JSON字符串
                    return@withContext jsonResponse
                }
            } catch (e: Exception) {
                // 网络请求失败
                e.printStackTrace()
            }
            // 接口错误或没有正常返回时，返回空字符串
            ""
        }
    }
}