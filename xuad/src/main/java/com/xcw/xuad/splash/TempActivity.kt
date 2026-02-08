package com.xcw.xuad.splash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.ad.TTAdManagerHolder
import com.xcw.xuad.log.XuLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import com.bytedance.sdk.openadsdk.CSJSplashAd

/**
 * 通用启动容器页：全屏背景图（或白底）+ 底部加载文案与指示器
 * - 如未通过 XuAdManager.init 传入图片，则白色背景
 * - 自动等待初始化完成后关闭自身
 */
class TempActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_THEME_COLOR = "extra_theme_color"
        private const val EXTRA_SPLASH_COUNT = "extra_splash_count"
        private const val EXTRA_LOADING_TEXT = "extra_loading_text"
        
        /**
         * 启动TempActivity
         * @param context 上下文
         * @param themeColor 主题色值（可选）
         * @param splashCount 开屏数量
         * @param loadingText 加载文案（可选，默认为"正在初始化应用..."）
         */
        fun start(context: Context, themeColor: Int? = null, splashCount: Int = 1, loadingText: String? = null) {
            val intent = Intent(context, TempActivity::class.java)
            themeColor?.let {
                intent.putExtra(EXTRA_THEME_COLOR, it)
            }
            intent.putExtra(EXTRA_SPLASH_COUNT, splashCount)
            loadingText?.let {
                intent.putExtra(EXTRA_LOADING_TEXT, it)
            }
            context.startActivity(intent)
        }
    }

    // 主题色：默认使用与主应用 ThemeColor 相同的值（若未传入参数）
    private var themeColor: Color = Color(0xFFBE0030)
    private var splashCount: Int = 1
    private var loadingText: String = "正在初始化应用..."
    private var adContainer: android.widget.FrameLayout? = null
    private var adShowing: Boolean by mutableStateOf(false)
    private var hasShownAnyAd: Boolean by mutableStateOf(false)
    private var nextPreloaded: CSJSplashAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 获取传入的主题色
        val colorArgb = intent.getIntExtra(EXTRA_THEME_COLOR, 0)
        if (colorArgb != 0) {
            themeColor = Color(colorArgb)
            XuLog.i("TempActivity 接收到主题色 ARGB: 0x%08X".format(colorArgb))
        } else {
            XuLog.w("TempActivity 未接收到主题色，使用默认值")
        }
        
        // 获取传入的加载文案
        val text = intent.getStringExtra(EXTRA_LOADING_TEXT)
        if (!text.isNullOrBlank()) {
            loadingText = text
        }

        setupFullScreen()
        // 读取开屏播放次数
        splashCount = intent.getIntExtra(EXTRA_SPLASH_COUNT, 1).coerceAtLeast(1)

        setContent {
            val img = XuAdManager.getSplashImageResId()
            if (img != null) {
                XuLog.i("TempActivity 使用开屏图资源: ${img}")
            } else {
                XuLog.w("TempActivity 未设置开屏图资源，显示白底")
            }
            TempScreen(img)
        }

        lifecycleScope.launch {
            // 等待资讯 SDK 初始化与容器就绪
            while (!TTAdManagerHolder.isInitialized()) {
                delay(100)
            }
            while (adContainer == null) {
                delay(50)
            }

            // 定义预加载函数：渲染成功后返回可展示的对象
            suspend fun preloadNext(): CSJSplashAd? {
                val d = kotlinx.coroutines.CompletableDeferred<CSJSplashAd?>()
                com.xcw.xuad.ad.SplashAdManager.preloadSplashAd(
                    act = this@TempActivity,
                    container = adContainer,
                    timeoutMs = 1500,
                    onReady = { d.complete(it) },
                    onFailed = { d.complete(null) }
                )
                return d.await()
            }

            // 预加载第一个资讯
            XuLog.i("开始预加载第 1 个资讯")
            nextPreloaded = preloadNext()
            XuLog.i("第 1 个资讯预加载完成")

            for (i in 1..splashCount) {
                XuLog.i("开始第 ${i}/${splashCount} 次开屏资讯")
                val cont = kotlinx.coroutines.CompletableDeferred<Unit>()

                // 展示当前（优先使用已预加载）
                val toShow = nextPreloaded
                
                // 立即开始预加载下一个资讯（在展示当前资讯的同时）
                val preloadJob = if (i < splashCount) {
                    lifecycleScope.launch {
                        XuLog.i("开始预加载第 ${i + 1} 个资讯")
                        nextPreloaded = preloadNext()
                        XuLog.i("第 ${i + 1} 个资讯预加载完成")
                    }
                } else {
                    nextPreloaded = null
                    null
                }
                if (toShow != null) {
                    XuLog.i("使用预加载的资讯展示第 ${i} 个")
                    com.xcw.xuad.ad.SplashAdManager.showSplashAd(
                        ad = toShow,
                        container = adContainer,
                        onShow = {
                            adShowing = true
                            hasShownAnyAd = true
                        },
                        onClosed = {
                            adShowing = false
                            cont.complete(Unit)
                        }
                    )
                } else {
                    // 无预加载则直接加载并展示（作为回退）
                    XuLog.i("预加载失败，直接加载第 ${i} 个资讯")
                    com.xcw.xuad.ad.SplashAdManager.loadSplashAd(
                        act = this@TempActivity,
                        container = adContainer,
                        timeoutMs = 1500,
                        onShow = {
                            adShowing = true
                            hasShownAnyAd = true
                        },
                        onClosed = {
                            adShowing = false
                            cont.complete(Unit)
                        },
                        onFailed = {
                            adShowing = false
                            cont.complete(Unit)
                        }
                    )
                }

                // 等待本次资讯关闭或失败
                cont.await()
                // 清理容器，避免残留视图影响层级
                try { adContainer?.removeAllViews() } catch (_: Exception) {}
                
                // 如果有下一个资讯，等待预加载完成
                if (i < splashCount && preloadJob != null) {
                    XuLog.i("等待第 ${i + 1} 个资讯预加载完成")
                    preloadJob.join()
                    XuLog.i("第 ${i + 1} 个资讯预加载等待完成")
                }
                
                // 资讯关闭后延迟 0.05s 再展示下一次（如果有）
                if (i < splashCount) {
                    delay(50)
                }
            }

            // 全部播放完成，结束页面
            XuLog.i("所有开屏资讯播放完成，关闭 TempActivity")
            finish()
        }
    }

    private fun setupFullScreen() {
        // 隐藏状态栏和导航栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 设置窗口标志
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    @Composable
    private fun TempScreen(imageResId: Int?) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 背景图层（与底部进度条作为同一层控制）：仅在首次展示前显示
            if (!adShowing) {
                if (imageResId != null) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = "启动页背景",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }
            }

            // 资讯承载容器（全屏覆盖）
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).apply {
                        // 设置调试背景色（半透明绿），用于验证容器是否被渲染
                        // 如果能看到绿色但看不到广告，说明容器在了，是广告View的问题（资源缺失）
                        setBackgroundColor(android.graphics.Color.parseColor("#3300FF00"))
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { fl ->
                    adContainer = fl
                },
                modifier = Modifier.fillMaxSize()
            )

            // 调试文本（仅在非发布版本或特殊调试下显示，这里为了排查问题强制显示版本号）
            Box(modifier = Modifier.padding(top = 40.dp, start = 20.dp)) {
                Text(
                    text = "XUAD SDK v4.2.5 (DebugMode)",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

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

            // 使用 Column 布局来放置进度条和文字：与背景同层，首次展示前显示
            if (!adShowing) {
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
                        text = loadingText,
                        color = themeColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}