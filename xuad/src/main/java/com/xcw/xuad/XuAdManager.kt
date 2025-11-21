package com.xcw.xuad

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.xcw.xuad.ad.AdStrategy
import com.xcw.xuad.log.XuLog
import com.xcw.xuad.ad.TTAdManagerHolder
import com.xcw.xuad.network.AppApi
import com.xcw.xuad.network.entity.ApiResult
import com.xcw.xuad.network.entity.AppInitRequest
import com.xcw.xuad.network.entity.AppInitResponse
import com.xcw.xuad.network.entity.AppUserInitRequest
import com.xcw.xuad.um.UmengManager
import com.xcw.xuad.network.NetworkConfig
import com.xcw.xuad.splash.TempActivity
import com.xcw.xuad.lifecycle.AppLifecycleManager

/**
 * 初始化步骤枚举
 */
enum class InitStep {
    APP_INIT,           // 应用初始化
    PARSE_AD_STRATEGY,  // 解析广告策略
    UMENG_PRE_INIT,     // 友盟预初始化
    UMENG_INIT,         // 友盟初始化
    GET_OAID,           // 获取OAID
    USER_INIT,          // 用户初始化
    AD_SDK_INIT,        // 广告SDK初始化
    COMPLETED           // 完成
}

/**
 * 初始化回调接口
 */
interface InitCallback {
    /**
     * 初始化步骤开始
     */
    fun onStepStart(step: InitStep) {}
    
    /**
     * 初始化步骤完成
     */
    fun onStepCompleted(step: InitStep) {}
    
    /**
     * 初始化步骤失败
     */
    fun onStepFailed(step: InitStep, error: Throwable) {}
    
    /**
     * 初始化全部完成
     */
    fun onInitCompleted() {}
    
    /**
     * 初始化失败
     */
    fun onInitFailed(error: Throwable) {}
    
    /**
     * 广告功能不可用时的回调
     * 当 adEnabled != 1 时调用，表示不需要广告功能，应用可以直接进入主界面
     */
    fun onAdDisabled() {}
}

/**
 * 单例广告管理器：负责SDK初始化与数据持有
 */
object XuAdManager {

    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var initResult: ApiResult<AppInitResponse>? = null
    @Volatile
    private var strategy: AdStrategy? = null
    @Volatile
    private var splashImageResId: Int? = null
    @Volatile
    private var themeColor: Int? = null
    
    // 初始化上下文数据
    private data class InitContext(
        val context: Context,
        val packageName: String,
        val version: String,
        val channelName: String,
        val callback: InitCallback?
    )
    
    private var initContext: InitContext? = null

    /**
     * 初始化SDK
     * @param context 应用上下文
     * @param channelName 渠道名称
     * @param splashResId 启动页图片资源ID（可选）
     * @param themeColor 主题色 ARGB 整数（可选）
     * @param callback 初始化回调（可选）
     */
    fun init(context: Context, channelName: String, splashResId: Int? = null, themeColor: Int? = null, callback: InitCallback? = null) {
        XuLog.i("SDK 初始化开始")
        splashImageResId = splashResId
        this.themeColor = themeColor
        
        val pkg = context.packageName
        val ver = getVersionNameCompat(context)
        val channel = channelName.trim().lowercase()
        
        initContext = InitContext(context, pkg, ver, channel, callback)
        
        // 开始第一步：应用初始化
        executeAppInit()
    }
    // ==================== 新的分步骤初始化方法 ====================
    
    /**
     * 步骤1：应用初始化（5秒超时，失败不阻断后续步骤）
     */
    private fun executeAppInit() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.APP_INIT)
        
        Thread {
            try {
                val res = AppApi.init(
                    AppInitRequest(
                        packageName = ctx.packageName,
                        channelName = ctx.channelName,
                        version = ctx.version
                    )
                )
                initResult = res
                initialized = true
                XuLog.i("SDK 初始化完成，success=${res.success}, code=${res}")
                
                ctx.callback?.onStepCompleted(InitStep.APP_INIT)
                
                // 进入下一步：友盟预初始化（调整顺序）
                executeUmengPreInit()
                
            } catch (e: Exception) {
                XuLog.e("SDK 初始化失败: ${e.message}")
                initialized = false
                // 记录失败，但不阻断整体初始化，直接进入下一步
                ctx.callback?.onStepFailed(InitStep.APP_INIT, e)
                executeUmengPreInit()
            }
        }.start()
    }
    
    
    
    /**
     * 步骤2：友盟预初始化
     */
    private fun executeUmengPreInit() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.UMENG_PRE_INIT)
        
        val umKey = initResult?.data?.umengKey ?: ""
        
        Handler(Looper.getMainLooper()).post {
            try {
                if (umKey.isNotBlank()) {
                    XuLog.i("友盟预初始化开始")
                    UmengManager.preInit(ctx.context, umKey, ctx.channelName)
                    XuLog.i("友盟预初始化完成")
                } else {
                    XuLog.w("友盟umKey为空，跳过友盟预初始化")
                }
                
                ctx.callback?.onStepCompleted(InitStep.UMENG_PRE_INIT)
                
                // 进入下一步：友盟初始化
                executeUmengInit()
                
            } catch (e: Exception) {
                XuLog.e("友盟预初始化失败: ${e.message}")
                ctx.callback?.onStepFailed(InitStep.UMENG_PRE_INIT, e)
                // 出错不阻断主流程：继续下一步
                XuLog.i("友盟预初始化失败，继续进行友盟初始化")
                executeUmengInit()
            }
        }
    }
    
    /**
     * 步骤3：友盟初始化
     */
    private fun executeUmengInit() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.UMENG_INIT)
        
        val umKey = initResult?.data?.umengKey ?: ""
        
        try {
            if (umKey.isNotBlank()) {
                XuLog.i("友盟初始化开始")
                UmengManager.umInit(ctx.context, umKey, ctx.channelName)
                XuLog.i("友盟初始化完成")
            } else {
                XuLog.w("友盟umKey为空，跳过友盟初始化")
            }
            
            ctx.callback?.onStepCompleted(InitStep.UMENG_INIT)
            
            // 进入下一步：获取OAID
            executeGetOaid()
            
        } catch (e: Exception) {
            XuLog.e("友盟初始化失败: ${e.message}")
            ctx.callback?.onStepFailed(InitStep.UMENG_INIT, e)
            // 出错不阻断主流程：继续下一步
            XuLog.i("友盟初始化失败，继续获取OAID")
            executeGetOaid()
        }
    }
    
    /**
     * 步骤4：获取OAID
     */
    private fun executeGetOaid() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.GET_OAID)
        
        UmengManager.getUmOaid(ctx.context) { oaid ->
            try {
                XuLog.i("获取OAID完成: ${oaid ?: ""}")
                oaidCache = oaid
                
                ctx.callback?.onStepCompleted(InitStep.GET_OAID)
                
                // 进入下一步：用户初始化
                executeUserInit()
                
            } catch (e: Exception) {
                XuLog.e("获取OAID失败: ${e.message}")
                ctx.callback?.onStepFailed(InitStep.GET_OAID, e)
                // 出错不阻断主流程：继续下一步
                XuLog.i("获取OAID失败或为空，继续进行策略校验")
                executeUserInit()
            }
        }
    }
    
    /**
     * 步骤5：广告策略校验（同步阻塞，等待接口返回再继续）
     * 使用新增接口 `/app/ad-strategy/check`，在获取到 OAID 后进行策略校验：
     * - 若返回的 `data` 为空字符串：不做处理
     * - 若返回的 `data` 非空：将其写入 `initResult.data.adStrategy`，用于后续解析替换广告策略
     */
    private fun executeUserInit() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.USER_INIT)
        
        // 同步执行策略校验：如果当前在主线程，则切到后台线程执行并阻塞等待
        val task = {
            try {
                val checkRes = AppApi.checkAdStrategy(
                    AppUserInitRequest(
                        packageName = ctx.packageName,
                        channelName = ctx.channelName,
                        version = ctx.version,
                        oaid = oaidCache ?: "",
                        deviceInfo = ""
                    )
                )
                val newRaw = checkRes.data ?: ""
                if (newRaw.isNotBlank()) {
                    // 将新策略原文写入 initResult.data.adStrategy（不可变数据类需要复制）
                    initResult = initResult?.let { old ->
                        val oldData = old.data
                        if (oldData != null) {
                            val newData = oldData.copy(adStrategy = newRaw)
                            old.copy(data = newData)
                        } else {
                            old
                        }
                    }
                    XuLog.i("策略校验返回新策略，已更新原始策略字符串")
                } else {
                    XuLog.i("策略校验返回空字符串，保持原始策略不变")
                }
            } catch (e: Exception) {
                XuLog.e("广告策略校验失败: ${e.message}")
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 在主线程：切后台执行并阻塞等待完成
            val t = Thread(task)
            t.start()
            try { t.join() } catch (_: InterruptedException) {}
        } else {
            // 已在后台线程：直接同步执行
            task()
        }

        // 策略校验返回后再进入下一步：解析广告策略（调整顺序）
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ctx.callback?.onStepCompleted(InitStep.USER_INIT)
            executeParseAdStrategy()
        } else {
            Handler(Looper.getMainLooper()).post {
                ctx.callback?.onStepCompleted(InitStep.USER_INIT)
                executeParseAdStrategy()
            }
        }
    }

    /**
     * 步骤6：解析广告策略
     */
    private fun executeParseAdStrategy() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.PARSE_AD_STRATEGY)
        
        try {
            // 仅当 adEnabled=1 时解析广告策略
            val adOn = (initResult?.data?.adEnabled == 1)
            if (adOn) {
                val raw = initResult?.data?.adStrategy ?: ""
                strategy = runCatching {
                    if (raw.isNotBlank()) NetworkConfig.json.decodeFromString(AdStrategy.serializer(), raw) else null
                }.onFailure { XuLog.w("广告策略解析失败: ${it.message}") }.getOrNull()
            } else {
                strategy = null
                XuLog.i("广告开关关闭，跳过广告策略解析与后续广告初始化")
            }
            
            ctx.callback?.onStepCompleted(InitStep.PARSE_AD_STRATEGY)
            
            // 进入下一步：广告SDK初始化（调整顺序）
            executeAdSdkInit()
            
        } catch (e: Exception) {
            XuLog.e("广告策略解析失败: ${e.message}")
            ctx.callback?.onStepFailed(InitStep.PARSE_AD_STRATEGY, e)
            // 出错不阻断主流程：继续下一步
            XuLog.i("广告策略解析失败，继续进行广告SDK初始化")
            executeAdSdkInit()
        }
    }
    
    /**
     * 步骤7：广告SDK初始化
     */
    private fun executeAdSdkInit() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.AD_SDK_INIT)
        
        try {
            if (adEnabled()) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        XuLog.i("开始初始化穿山甲聚合SDK")
                        TTAdManagerHolder.initMediationAdSdk(ctx.context)
                        
                        ctx.callback?.onStepCompleted(InitStep.AD_SDK_INIT)
                        
                        // 所有步骤完成
                        executeCompleted()
                        
                    } catch (e: Exception) {
                        XuLog.e("穿山甲聚合SDK初始化失败: ${e.message}")
                        ctx.callback?.onStepFailed(InitStep.AD_SDK_INIT, e)
                        // 出错不阻断主流程：继续完成流程
                        XuLog.i("广告SDK初始化失败，继续完成整体初始化流程")
                        executeCompleted()
                    }
                }
            } else {
                XuLog.i("广告关闭，跳过广告SDK初始化")
                ctx.callback?.onStepCompleted(InitStep.AD_SDK_INIT)
                
                // 通知广告功能不可用
                ctx.callback?.onAdDisabled()
                
                // 所有步骤完成
                executeCompleted()
            }
            
        } catch (e: Exception) {
            XuLog.e("广告SDK初始化失败: ${e.message}")
            ctx.callback?.onStepFailed(InitStep.AD_SDK_INIT, e)
            // 出错不阻断主流程：继续完成流程
            XuLog.i("广告SDK初始化失败（外层），继续完成整体初始化流程")
            executeCompleted()
        }
    }
    
    /**
     * 步骤8：初始化完成
     */
    private fun executeCompleted() {
        val ctx = initContext ?: return
        ctx.callback?.onStepStart(InitStep.COMPLETED)
        
        XuLog.i("SDK 初始化流程全部完成")
        
        // 如果广告开启且策略允许全局开屏，跳转到 TempActivity
        if (adEnabled()) {
            val enableGlobalSplash = strategy?.globalSplashEnabled == true
            XuLog.i("广告已开启，globalSplashEnabled=${enableGlobalSplash}")
            if (enableGlobalSplash) {
                val count = (strategy?.splashCount ?: 1).coerceAtLeast(1)
                XuLog.i("启动 TempActivity，splashCount=${count}")
                Handler(Looper.getMainLooper()).post {
                    TempActivity.start(ctx.context, themeColor, splashCount = count)
                }
            } else {
                XuLog.i("策略未启用全局开屏，跳过 TempActivity")
            }
        }
        
        // 初始化热启动管理器
        try {
            val application = ctx.context.applicationContext as? android.app.Application
            if (application != null) {
                AppLifecycleManager.initialize(application)
                XuLog.i("AppLifecycleManager 初始化成功")
            } else {
                XuLog.e("AppLifecycleManager 初始化失败: 无法获取Application实例")
            }
        } catch (e: Exception) {
            XuLog.e("AppLifecycleManager 初始化失败: ${e.message}")
        }
        
        ctx.callback?.onStepCompleted(InitStep.COMPLETED)
        ctx.callback?.onInitCompleted()
        
        // 清理初始化上下文
        initContext = null
    }

    private fun getVersionNameCompat(context: Context): String {
        return try {
            val pm = context.packageManager
            val packageName = context.packageName
            val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getPackageInfo(packageName, 0)
            }
            @Suppress("DEPRECATION") info.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // 状态与数据访问
    fun isInitialized(): Boolean = initialized
    fun getInitResult(): ApiResult<AppInitResponse>? = initResult
    fun getInitData(): AppInitResponse? = initResult?.data
    fun getAdStrategy(): AdStrategy? = strategy
    fun getSplashImageResId(): Int? = splashImageResId
    fun getThemeColor(): Int? = themeColor
    private var oaidCache: String? = null
    private var userInitId: Long? = null
    fun getOaid(): String? = oaidCache
    fun getUserInitId(): Long? = userInitId

    // 便捷字段访问
    fun adEnabled(): Boolean = (initResult?.data?.adEnabled == 1)
    fun adStrategy(): AdStrategy? = strategy
    fun pangleAppId(): String = initResult?.data?.pangleAppId ?: ""
    fun adSplashId(): String = initResult?.data?.adSplashId ?: ""
    fun adInterstitialId(): String = initResult?.data?.adInterstitialId ?: ""
    fun adBannerId(): String = initResult?.data?.adBannerId ?: ""
    fun adRewardedId(): String = initResult?.data?.adRewardedId ?: ""
    fun downloadUrl(): String = initResult?.data?.downloadUrl ?: ""
    fun umengKey(): String = initResult?.data?.umengKey ?: ""
    fun apihzId(): String = initResult?.data?.apihzId ?: ""
    fun apihzKey(): String = initResult?.data?.apihzKey ?: ""

    // 应用基础信息（供上报使用）
    fun appPackageName(): String = initContext?.packageName ?: ""
    fun appVersion(): String = initContext?.version ?: ""
    fun appChannelName(): String = initContext?.channelName ?: ""
}