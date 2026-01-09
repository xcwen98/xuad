package com.xcw.xuad.adHelp

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xcw.xuad.ad.RealTimeMonitoring
import com.xcw.xuad.ad.BannerAdManager
import kotlin.apply

/**
 * Banner广告容器组件
 *
 * 功能特性：
 * - 支持页面标识绑定，与RealTimeMonitoring联动
 * - 自动管理广告加载和显示状态
 * - 提供优雅的加载状态和错误处理
 * - 支持自定义样式和布局
 *
 * @param pageIdentifier 页面标识，用于与RealTimeMonitoring绑定
 * @param modifier 修饰符
 * @param showLoadingIndicator 是否显示加载指示器
 * @param backgroundColor 背景颜色
 */
@Composable
fun BannerAdContainer(
    pageIdentifier: String,
    modifier: Modifier = Modifier,
    showLoadingIndicator: Boolean = true,
    backgroundColor: Color = Color.Transparent
) {
    val context = LocalContext.current

    // 广告显示状态
    var shouldShowAd by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // 注册banner容器回调
    LaunchedEffect(pageIdentifier) {
        RealTimeMonitoring.registerBannerContainer(pageIdentifier) { showAd ->
            shouldShowAd = showAd
            if (showAd) {
                isLoading = true
                hasError = false
            }
        }
    }

    // 页面销毁时注销回调
    DisposableEffect(pageIdentifier) {
        onDispose {
            RealTimeMonitoring.unregisterBannerContainer(pageIdentifier)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 控制内容，首先是上下外边距是0
            .padding(vertical = 0.dp)
    ) {
        when {
            !shouldShowAd -> {
                Spacer(modifier = Modifier.height(0.dp))
            }

            hasError -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RectangleShape, // 采用直角的设计
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "模块加载失败",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            else -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RectangleShape, // 采用直角的设计
                    colors = CardDefaults.cardColors(containerColor = backgroundColor), // 背景色为透明 (默认)
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 去掉圆角通常也意味着不需要阴影，或者阴影需要直角。这里设为0dp以完全透明
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        AndroidView(
                            factory = { context ->
                                val container = FrameLayout(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }

                                if (context is Activity) {
                                    BannerAdManager.loadBannerAd(context, container) { success ->
                                        isLoading = false
                                        if (!success) {
                                            hasError = true
                                        }
                                    }
                                } else {
                                    hasError = true
                                    isLoading = false
                                }

                                container
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isLoading && showLoadingIndicator) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(backgroundColor), // 透明背景
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "核心模块加载中，请稍后...",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
