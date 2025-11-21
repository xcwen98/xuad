package com.xcw.xuad.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.xcw.xuad.XuAdManager
import com.xcw.xuad.log.XuLog
import com.xcw.xuad.splash.TempActivity

/**
 * 应用生命周期管理器
 * 负责监听应用的前后台切换，实现热启动资讯功能
 */
object AppLifecycleManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    
    private var isAppInForeground = false
    private var isFirstLaunch = true
    private var currentActivity: Activity? = null
    private var hotStartCallback: (() -> Unit)? = null
    
    // 热启动资讯显示次数限制
    private var hotStartAdShownCount = 0
    private val maxHotStartAdCount = 1
    
    /**
     * 初始化生命周期监听
     * @param application 应用实例
     * @param callback 热启动回调（可选）
     */
    fun initialize(application: Application, callback: (() -> Unit)? = null) {
        XuLog.i("AppLifecycleManager: 初始化生命周期监听")
        hotStartCallback = callback
        
        // 监听进程生命周期（前后台切换）
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // 监听Activity生命周期（获取当前Activity）
        application.registerActivityLifecycleCallbacks(this)
    }
    
    /**
     * 重置热启动资讯计数（通常在应用重新安装或清除数据时调用）
     */
    fun resetHotStartAdCount() {
        hotStartAdShownCount = 0
        XuLog.i("AppLifecycleManager: 重置热启动资讯计数")
    }
    
    /**
     * 获取当前热启动资讯显示次数
     */
    fun getHotStartAdCount(): Int = hotStartAdShownCount
    
    // ==================== ProcessLifecycleOwner 回调 ====================
    
    override fun onStart(owner: LifecycleOwner) {
        XuLog.i("AppLifecycleManager: 应用进入前台")
        
        if (!isFirstLaunch && !isAppInForeground) {
            // 这是一次热启动，每次都触发检测
            XuLog.i("AppLifecycleManager: 检测到热启动")
            
            // 延迟一段时间确保Activity生命周期回调完成
            Handler(Looper.getMainLooper()).postDelayed({
                handleHotStart()
            }, 300)
        }
        
        isAppInForeground = true
        isFirstLaunch = false
    }
    
    override fun onStop(owner: LifecycleOwner) {
        XuLog.i("AppLifecycleManager: 应用进入后台")
        isAppInForeground = false
    }
    
    // ==================== Activity 生命周期回调 ====================
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // 不需要处理
    }
    
    override fun onActivityStarted(activity: Activity) {
        // 不需要处理
    }
    
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        XuLog.d("AppLifecycleManager: 当前Activity: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
    
    override fun onActivityStopped(activity: Activity) {
        // 不需要处理
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // 不需要处理
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
    
    // ==================== 热启动处理逻辑 ====================
    
    /**
     * 处理热启动逻辑
     */
    private fun handleHotStart(retryCount: Int = 0) {
        XuLog.i("AppLifecycleManager: 开始处理热启动逻辑")
        
        // 检查资讯是否开启
        if (!XuAdManager.adEnabled()) {
            XuLog.i("AppLifecycleManager: 资讯开关关闭，跳过热启动资讯")
            return
        }
        
        // 检查热启动资讯配置
        val strategy = XuAdManager.getAdStrategy()
        if (strategy?.hotStartSplash != true) {
            XuLog.i("AppLifecycleManager: hotStartSplash配置为false，跳过热启动资讯")
            return
        }
        
        // 获取当前Activity作为上下文
        val context = currentActivity
        if (context == null) {
            if (retryCount < 3) {
                XuLog.w("AppLifecycleManager: 当前Activity为null，等待Activity准备好... (重试 ${retryCount + 1}/3)")
                Handler(Looper.getMainLooper()).postDelayed({
                    handleHotStart(retryCount + 1)
                }, 200)
                return
            } else {
                XuLog.w("AppLifecycleManager: 等待Activity超时，无法启动热启动资讯")
                return
            }
        }
        
        XuLog.i("AppLifecycleManager: 准备显示热启动资讯")
        
        // 延迟一小段时间确保Activity完全恢复
        Handler(Looper.getMainLooper()).postDelayed({
            showHotStartAd(context)
        }, 500)
    }
    
    /**
     * 显示热启动资讯
     * @param context 当前Activity上下文
     */
    private fun showHotStartAd(context: Context) {
        try {
            
            // 获取主题色
            val themeColor = XuAdManager.getThemeColor()
            
            // 启动TempActivity，splashCount固定为1
            TempActivity.start(context, themeColor, splashCount = 1)
            
            // 调用自定义回调
            hotStartCallback?.invoke()
            
        } catch (e: Exception) {
            XuLog.e("AppLifecycleManager: 启动热启动资讯失败: ${e.message}")
        }
    }
}