package com.xcw.xuad.ad

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.log.XuLog
import java.util.concurrent.ConcurrentHashMap

/**
 * 实时广告监控类
 * 负责根据页面标识和广告策略，实时监控和执行广告展示逻辑
 */
object RealTimeMonitoring {
    
    // 页面广告状态管理
    private val pageAdStates = ConcurrentHashMap<String, PageAdState>()
    
    // 主线程Handler，用于延时任务
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Banner广告容器回调接口
    private val bannerContainerCallbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()
    
    /**
     * 页面广告状态数据类
     * @param pageName 页面标识
     * @param isActive 页面是否活跃（用于判断是否需要取消未执行的广告）
     * @param interstitialExecutedCount 已执行的插屏广告次数
     * @param bannerExecutedCount 已执行的banner广告次数
     * @param pendingRunnables 待执行的延时任务列表
     */
    private data class PageAdState(
        val pageName: String,
        var isActive: Boolean = true,
        var interstitialExecutedCount: Int = 0,
        var bannerExecutedCount: Int = 0,
        val pendingRunnables: MutableList<Runnable> = mutableListOf()
    )
    
    /**
     * 雷核追踪方法 - 核心入口
     * 当页面执行到特定位置时调用此方法，触发广告策略执行
     * 
     * @param context Android上下文
     * @param pageIdentifier 页面标识字符串
     */
    fun XuTrack(context: Context, pageIdentifier: String) {
        XuLog.d("XuTrack called for page: $pageIdentifier")
        
        // 获取广告策略
        val adStrategy = XuAdManager.getAdStrategy()
        if (adStrategy == null) {
            XuLog.w("AdStrategy is null, cannot execute tracking for page: $pageIdentifier")
            return
        }
        
        // 初始化或更新页面状态
        val pageState = pageAdStates.getOrPut(pageIdentifier) { 
            PageAdState(pageIdentifier)
        }
        pageState.isActive = true
        pageState.interstitialExecutedCount = 0
        pageState.bannerExecutedCount = 0
        pageState.pendingRunnables.clear()
        
        // 获取页面对应的广告配置
        val pageConfig = getPageAdConfig(adStrategy, pageIdentifier)
        if (pageConfig == null) {
            XuLog.w("No ad config found for page: $pageIdentifier")
            return
        }
        
        XuLog.d("Found ad config for page $pageIdentifier: mode=${pageConfig.interstitialMode}, count=${pageConfig.interstitialCount}, bannerAd=${pageConfig.bannerAd}")
        
        // 执行插屏广告策略（仅在全局启用时）
        if (adStrategy.globalInterstitialEnabled) {
            executeInterstitialStrategy(context, pageState, pageConfig, adStrategy)
        } else {
            XuLog.d("Global interstitial ads disabled, skipping interstitial for page: $pageIdentifier")
        }
        
        // 执行banner广告策略（独立于插屏广告控制）
        executeBannerStrategy(context, pageState, pageConfig, adStrategy)
    }
    
    /**
     * 页面离开时调用，清理相关状态
     * 
     * @param pageIdentifier 页面标识
     */
    fun onPageLeave(pageIdentifier: String) {
        XuLog.d("Page leave: $pageIdentifier")
        
        val pageState = pageAdStates[pageIdentifier]
        if (pageState != null) {
            pageState.isActive = false
            
            // 取消所有待执行的延时任务
            pageState.pendingRunnables.forEach { runnable ->
                mainHandler.removeCallbacks(runnable)
            }
            pageState.pendingRunnables.clear()
            
            XuLog.d("Cancelled pending ads for page: $pageIdentifier")
        }
        
        // 注销banner容器回调
        unregisterBannerContainer(pageIdentifier)
    }
    
    /**
     * 获取页面对应的广告配置
     * 优先从detailConfigs中匹配，如果没有匹配则使用fallbackConfig
     * 
     * @param adStrategy 广告策略
     * @param pageIdentifier 页面标识
     * @return 匹配的广告配置，如果都没有则返回null
     */
    private fun getPageAdConfig(adStrategy: AdStrategy, pageIdentifier: String): AdPageConfig? {
        // 首先尝试从detailConfigs中匹配
        val matchedConfig = adStrategy.detailConfigs.find { config ->
            config.pageName == pageIdentifier
        }
        
        if (matchedConfig != null) {
            XuLog.d("Found matched config in detailConfigs for page: $pageIdentifier")
            return matchedConfig
        }
        
        // 如果没有匹配，使用fallbackConfig
        val fallbackConfig = adStrategy.fallbackConfig
        if (fallbackConfig != null) {
            XuLog.d("Using fallback config for page: $pageIdentifier")
            return fallbackConfig
        }
        
        return null
    }
    
    /**
     * 执行插屏广告策略
     * 根据配置的模式（cooldown或concurrent）执行不同的广告加载逻辑
     * 
     * @param context Android上下文
     * @param pageState 页面状态
     * @param pageConfig 页面广告配置
     * @param adStrategy 全局广告策略
     */
    private fun executeInterstitialStrategy(
        context: Context,
        pageState: PageAdState,
        pageConfig: AdPageConfig,
        adStrategy: AdStrategy
    ) {
        val targetCount = pageConfig.interstitialCount
        if (targetCount <= 0) {
            XuLog.d("Interstitial count is 0, skipping ads for page: ${pageState.pageName}")
            return
        }
        
        when (pageConfig.interstitialMode.lowercase()) {
            "cooldown" -> {
                executeCooldownMode(context, pageState, pageConfig, adStrategy, targetCount)
            }
            "concurrent" -> {
                executeConcurrentMode(context, pageState, pageConfig, targetCount)
            }
            else -> {
                XuLog.w("Unknown interstitial mode: ${pageConfig.interstitialMode}, using cooldown as default")
                executeCooldownMode(context, pageState, pageConfig, adStrategy, targetCount)
            }
        }
    }
    
    /**
     * 执行Cooldown模式的插屏广告
     * 必须上一个广告关闭了，才会开始计时，然后到时间加载广告
     * 
     * @param context Android上下文
     * @param pageState 页面状态
     * @param pageConfig 页面广告配置
     * @param adStrategy 全局广告策略
     * @param targetCount 目标广告次数
     */
    private fun executeCooldownMode(
        context: Context,
        pageState: PageAdState,
        pageConfig: AdPageConfig,
        adStrategy: AdStrategy,
        targetCount: Int
    ) {
        XuLog.d("Executing cooldown mode for page: ${pageState.pageName}, target count: $targetCount")
        
        val cooldownSeconds = adStrategy.interstitialCooldown
        
        fun showNextInterstitial() {
            // 检查页面是否仍然活跃
            if (!pageState.isActive) {
                XuLog.d("Page ${pageState.pageName} is no longer active, stopping cooldown ads")
                return
            }
            
            // 检查是否已达到目标次数
            if (pageState.interstitialExecutedCount >= targetCount) {
                XuLog.d("Reached target count $targetCount for page: ${pageState.pageName}")
                return
            }
            
            // 展示插屏广告，并在广告关闭后开始计时
            showInterstitialAdWithCloseCallback(context, pageState.pageName, 
                onShow = { success ->
                    if (success) {
                        pageState.interstitialExecutedCount++
                        XuLog.d("Interstitial ad shown successfully for page: ${pageState.pageName}, count: ${pageState.interstitialExecutedCount}/$targetCount")
                    } else {
                        XuLog.w("Failed to show interstitial ad for page: ${pageState.pageName}")
                    }
                },
                onClosed = {
                    XuLog.d("Interstitial ad closed for page: ${pageState.pageName}, starting cooldown timer")
                    
                    // 广告关闭后，如果还没达到目标次数，开始冷却计时
                    if (pageState.interstitialExecutedCount < targetCount && pageState.isActive) {
                        val delayRunnable = Runnable {
                            showNextInterstitial()
                        }
                        
                        pageState.pendingRunnables.add(delayRunnable)
                        mainHandler.postDelayed(delayRunnable, cooldownSeconds * 1000L)
                        
                        XuLog.d("Scheduled next interstitial ad for page: ${pageState.pageName} after ${cooldownSeconds}s cooldown")
                    }
                }
            )
        }
        
        // 立即展示第一个广告
        showNextInterstitial()
    }
    
    /**
     * 执行Concurrent模式的插屏广告
     * 并发加载：一股脑加载所有广告，每个广告间隔0.5秒
     * 
     * @param context Android上下文
     * @param pageState 页面状态
     * @param pageConfig 页面广告配置
     * @param targetCount 目标广告次数
     */
    /**
     * 执行Concurrent模式的插屏广告
     * 每间隔0.5秒加载一次广告，不管其他广告的状态
     * 
     * @param context Android上下文
     * @param pageState 页面状态
     * @param pageConfig 页面广告配置
     * @param targetCount 目标广告次数
     */
    private fun executeConcurrentMode(
        context: Context,
        pageState: PageAdState,
        pageConfig: AdPageConfig,
        targetCount: Int
    ) {
        XuLog.d("Executing concurrent mode for page: ${pageState.pageName}, target count: $targetCount")
        
        var currentCount = 0
        
        fun scheduleNextAd() {
            if (!pageState.isActive) {
                XuLog.d("Page ${pageState.pageName} is no longer active, stopping concurrent ads")
                return
            }
            
            if (currentCount >= targetCount) {
                XuLog.d("Reached target count $targetCount for concurrent mode on page: ${pageState.pageName}")
                return
            }
            
            val adIndex = currentCount
            currentCount++
            
            // 加载广告
            showInterstitialAd(context, pageState.pageName) { success ->
                if (success) {
                    pageState.interstitialExecutedCount++
                    XuLog.d("Concurrent interstitial ad $adIndex shown for page: ${pageState.pageName}, total count: ${pageState.interstitialExecutedCount}")
                } else {
                    XuLog.w("Failed to show concurrent interstitial ad $adIndex for page: ${pageState.pageName}")
                }
            }
            
            // 如果还有更多广告要加载，0.5秒后继续
            if (currentCount < targetCount) {
                val nextRunnable = Runnable {
                    scheduleNextAd()
                }
                
                pageState.pendingRunnables.add(nextRunnable)
                mainHandler.postDelayed(nextRunnable, 500L)
                
                XuLog.d("Scheduled next concurrent ad for page: ${pageState.pageName} in 0.5s (${currentCount}/$targetCount)")
            }
        }
        
        // 立即开始第一个广告
        scheduleNextAd()
    }
    
    /**
     * 加载并显示插屏广告
     * 
     * @param context Android上下文
     * @param pageName 页面名称（用于日志）
     * @param callback 广告展示结果回调
     */
    private fun showInterstitialAd(context: Context, pageName: String, callback: (Boolean) -> Unit) {
        XuLog.d("Attempting to load and show interstitial ad for page: $pageName")
        
        // 检查Context是否为Activity类型
        if (context !is android.app.Activity) {
            XuLog.w("Context is not an Activity, cannot show interstitial ad for page: $pageName")
            callback(false)
            return
        }
        
        try {
            // 调用插屏广告管理器加载并展示广告
            InterstitialAdManager.loadInterstitialFullAd(
                context,
                onShow = {
                    XuLog.d("Interstitial ad shown for page: $pageName")
                    callback(true)
                },
                onClosed = {
                    XuLog.d("Interstitial ad closed for page: $pageName")
                },
                onFailed = {
                    XuLog.w("Interstitial ad failed to load for page: $pageName")
                    callback(false)
                }
            )
        } catch (e: Exception) {
            XuLog.e("Error loading interstitial ad for page $pageName: ${e.message}")
            callback(false)
        }
    }
    
    /**
     * 加载并显示插屏广告（带关闭回调）
     * 
     * @param context Android上下文
     * @param pageName 页面名称（用于日志）
     * @param onShow 广告展示回调
     * @param onClosed 广告关闭回调
     */
    private fun showInterstitialAdWithCloseCallback(
        context: Context, 
        pageName: String, 
        onShow: (Boolean) -> Unit,
        onClosed: () -> Unit
    ) {
        XuLog.d("Attempting to load and show interstitial ad with close callback for page: $pageName")
        
        // 检查Context是否为Activity类型
        if (context !is android.app.Activity) {
            XuLog.w("Context is not an Activity, cannot show interstitial ad for page: $pageName")
            onShow(false)
            return
        }
        
        try {
            // 调用插屏广告管理器加载并展示广告
            InterstitialAdManager.loadInterstitialFullAd(
                context,
                onShow = {
                    XuLog.d("Interstitial ad shown for page: $pageName")
                    onShow(true)
                },
                onClosed = {
                    XuLog.d("Interstitial ad closed for page: $pageName")
                    onClosed()
                },
                onFailed = {
                    XuLog.w("Interstitial ad failed to load for page: $pageName")
                    onShow(false)
                }
            )
        } catch (e: Exception) {
            XuLog.e("Error loading interstitial ad for page $pageName: ${e.message}")
            onShow(false)
        }
    }
    
    /**
     * 执行banner广告策略
     * 根据全局banner开关和页面配置决定是否加载banner广告
     * 
     * @param context Android上下文
     * @param pageState 页面状态
     * @param pageConfig 页面广告配置
     * @param adStrategy 全局广告策略
     */
    private fun executeBannerStrategy(
        context: Context,
        pageState: PageAdState,
        pageConfig: AdPageConfig,
        adStrategy: AdStrategy
    ) {
        // 检查全局banner广告是否启用
        if (!adStrategy.globalBannerEnabled) {
            XuLog.d("Global banner ads disabled, skipping page: ${pageState.pageName}")
            return
        }
        
        // 检查页面配置是否启用banner广告
        if (!pageConfig.bannerAd) {
            XuLog.d("Banner ads disabled for page: ${pageState.pageName}")
            return
        }
        
        XuLog.d("Executing banner strategy for page: ${pageState.pageName}")
        
        // 检查Context是否为Activity类型
        if (context !is android.app.Activity) {
            XuLog.w("Context is not an Activity, cannot load banner ad for page: ${pageState.pageName}")
            return
        }
        
        // 延迟触发banner广告，确保容器回调已注册
        val delayRunnable = Runnable {
            if (pageState.isActive) {
                triggerBannerAdDisplay(pageState.pageName)
                pageState.bannerExecutedCount++
                XuLog.d("Banner ad triggered for page: ${pageState.pageName}, count: ${pageState.bannerExecutedCount}")
            }
        }
        
        pageState.pendingRunnables.add(delayRunnable)
        mainHandler.postDelayed(delayRunnable, 100) // 延迟100ms确保容器已注册
    }
    
    /**
     * 触发banner广告显示
     * 通知对应页面的banner容器开始加载广告
     * 
     * @param pageIdentifier 页面标识
     */
    private fun triggerBannerAdDisplay(pageIdentifier: String) {
        val callback = bannerContainerCallbacks[pageIdentifier]
        if (callback != null) {
            XuLog.d("Triggering banner ad display for page: $pageIdentifier")
            callback(true)
        } else {
            XuLog.w("No banner container callback found for page: $pageIdentifier")
        }
    }
    
    /**
     * 注册banner广告容器回调
     * 
     * @param pageIdentifier 页面标识
     * @param callback 回调函数，参数为是否显示广告
     */
    fun registerBannerContainer(pageIdentifier: String, callback: (Boolean) -> Unit) {
        bannerContainerCallbacks[pageIdentifier] = callback
        XuLog.d("Registered banner container callback for page: $pageIdentifier")
    }
    
    /**
     * 注销banner广告容器回调
     * 
     * @param pageIdentifier 页面标识
     */
    fun unregisterBannerContainer(pageIdentifier: String) {
        bannerContainerCallbacks.remove(pageIdentifier)
        XuLog.d("Unregistered banner container callback for page: $pageIdentifier")
    }

    /**
     * 获取页面当前的广告执行状态（用于调试）
     * 
     * @param pageIdentifier 页面标识
     * @return 页面广告状态信息
     */
    fun getPageAdState(pageIdentifier: String): String {
        val state = pageAdStates[pageIdentifier]
        return if (state != null) {
            "Page: ${state.pageName}, Active: ${state.isActive}, Interstitial: ${state.interstitialExecutedCount}, Banner: ${state.bannerExecutedCount}, Pending: ${state.pendingRunnables.size}"
        } else {
            "Page: $pageIdentifier not found"
        }
    }
    
    /**
     * 清理所有页面状态（用于测试或重置）
     */
    fun clearAllStates() {
        pageAdStates.values.forEach { state ->
            state.pendingRunnables.forEach { runnable ->
                mainHandler.removeCallbacks(runnable)
            }
        }
        pageAdStates.clear()
        bannerContainerCallbacks.clear()
        XuLog.d("Cleared all page ad states and banner container callbacks")
    }
}