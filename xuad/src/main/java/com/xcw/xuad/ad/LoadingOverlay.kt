package com.xcw.xuad.ad

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.ViewGroup
import android.content.res.ColorStateList

/**
 * 全屏加载蒙版与居中弹窗
 */
internal object LoadingOverlay {
    @Volatile
    private var dialog: Dialog? = null

    fun show(activity: Activity) {
        if (activity.isFinishing) return
        if (dialog?.isShowing == true) return
        activity.runOnUiThread {
            val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
            d.setCancelable(false)

            val root = FrameLayout(activity)
            root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // 半透明蒙版
            root.setBackgroundColor(Color.parseColor("#80000000"))

            // 居中白色圆角弹窗
            val box = LinearLayout(activity)
            box.orientation = LinearLayout.VERTICAL
            box.gravity = Gravity.CENTER
            val padding = (16 * activity.resources.displayMetrics.density).toInt()
            box.setPadding(padding, padding, padding, padding)
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.RECTANGLE
            bg.setColor(Color.WHITE)
            bg.cornerRadius = 12 * activity.resources.displayMetrics.density
            box.background = bg

            val progress = ProgressBar(activity)
            progress.isIndeterminate = true
            progress.indeterminateTintList = ColorStateList.valueOf(Color.BLACK)
            val tv = TextView(activity)
            tv.text = "正在加载关键组件，请稍等"
            tv.setTextColor(Color.BLACK)
            tv.textSize = 16f

            val lpWrap = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            box.addView(progress, lpWrap)
            val lpText = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lpText.topMargin = (8 * activity.resources.displayMetrics.density).toInt()
            box.addView(tv, lpText)

            val centerParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            centerParams.gravity = Gravity.CENTER
            root.addView(box, centerParams)

            d.setContentView(root)
            runCatching { d.show() }
            dialog = d
        }
    }

    fun dismiss() {
        val d = dialog ?: return
        dialog = null
        Handler(Looper.getMainLooper()).post {
            runCatching { if (d.isShowing) d.dismiss() }
        }
    }
}