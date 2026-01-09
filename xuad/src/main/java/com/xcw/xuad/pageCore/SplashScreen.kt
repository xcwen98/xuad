package com.xcw.xuad.pageCore

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 启动页UI组件
 * 使用Jetpack Compose显示全屏开屏图片和加载进度条
 */
@Composable
fun SplashScreen(
    bgResId: Int,
    themeColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 显示开屏背景图片
        Image(
            painter = painterResource(id = bgResId),
            contentDescription = "启动页背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )


        // 创建无限循环的进度条动画
        val infiniteTransition = rememberInfiniteTransition(label = "loading_progress")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress_animation"
        )

        // 使用 Column 布局来放置进度条和文字
        Column(
            modifier =  Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 进度条
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp),
                color = themeColor,
                trackColor = themeColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 加载提示文字
            Text(
                text = "正在初始化应用...",
                color = themeColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}