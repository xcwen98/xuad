package com.xcw.xuad.um

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.xcw.xuad.log.XuLog
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.listener.OnGetOaidListener
import java.security.MessageDigest

object UmengManager {
    fun preInit(context: Context, umKey: String, channelName: String) {
        UMConfigure.preInit(context, umKey, channelName)
    }

    fun umInit(context: Context, umKey: String, channelName: String) {
        // 设备类型：PHONE；推送secret传null
        UMConfigure.init(context, umKey, channelName, UMConfigure.DEVICE_TYPE_PHONE, null)
    }

    fun getUmOaid(context: Context, onOaid: (String?) -> Unit) {
        UMConfigure.getOaid(context, object : OnGetOaidListener {
            override fun onGetOaid(oaid: String?) {
                val finalId = oaid?.takeIf { it.isNotBlank() }
                    ?: ("LH-" + generateStableDeviceId(context))
                XuLog.i("OAID: $finalId")
                onOaid(finalId)
            }
        })
    }

    // 生成稳定的16位字符串（尽量设备级稳定）：优先 ANDROID_ID，不存在则拼接硬件指纹并哈希
    private fun generateStableDeviceId(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
        val seed = if (androidId.isNotBlank()) androidId
        else listOf(
            Build.FINGERPRINT,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.BRAND,
            Build.VERSION.INCREMENTAL
        ).joinToString("|")

        val hex = sha256Hex(seed)
        return if (hex.length >= 16) hex.substring(0, 16) else hex.padEnd(16, '0')
    }

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            val hi = v ushr 4
            val lo = v and 0x0F
            sb.append("0123456789abcdef"[hi])
            sb.append("0123456789abcdef"[lo])
        }
        return sb.toString()
    }
}