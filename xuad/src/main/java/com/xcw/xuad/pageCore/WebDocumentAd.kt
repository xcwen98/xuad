package com.xcw.xuad.pageCore

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.apply
import kotlin.jvm.java

/**
 * WebView页面Activity
 * 使用Jetpack Compose + WebView展示网页内容
 */
class WebDocumentAd : ComponentActivity() {
    
    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        
        /**
         * 启动WebView页面
         * @param context 上下文
         * @param url 要打开的网址
         * @param title 页面标题（可选）
         */
        fun start(context: Context, url: String, title: String = "网页") {
            val intent = Intent(context, WebDocumentAd::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val url = intent.getStringExtra(EXTRA_URL) ?: "https://www.baidu.com"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "网页"
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebViewScreen(
                        url = url,
                        title = title,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

/**
 * WebView页面Compose组件
 * @param url 要显示的网址
 * @param title 页面标题
 * @param onBackClick 返回按钮点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                TextButton(onClick = onBackClick) {
                    Text(
                        text = "← 返回",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        // 加载进度条
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
        
        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress == 100) {
                                isLoading = false
                            }
                        }
                    }
                    settings.apply {
                        // 启用 JavaScript
                        javaScriptEnabled = true
                        // 启用 DOM storage
                        domStorageEnabled = true
                        // 设置缓存模式
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // 支持缩放
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        // 混合内容模式 (允许HTTPS页面加载HTTP内容)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        // 允许文件访问
                        allowFileAccess = true
                    }
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.url != url) {
                    view.loadUrl(url)
                }
            }
        )
    }
}