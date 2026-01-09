package com.xcw.xuad.adHelp

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.xcw.xuad.ad.RealTimeMonitoring
import kotlin.let

object PageMonitoringHelper {

    @Composable
    fun MonitorPage(pageName: String, showLoading: Boolean = true) {
        val context = LocalContext.current
        val activity = context as? ComponentActivity

        LaunchedEffect(pageName) {
            activity?.let {
                try {
                    RealTimeMonitoring.XuTrack(it, pageName, showLoading)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        DisposableEffect(pageName) {
            onDispose {
                try {
                    RealTimeMonitoring.onPageLeave(pageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
