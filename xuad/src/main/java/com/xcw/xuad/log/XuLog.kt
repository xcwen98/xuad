package com.xcw.xuad.log

import android.util.Log

/**
 * 雷核日志工具类
 * 统一管理项目中的日志输出，所有日志前缀均为"雷核"
 */
class XuLog {
    companion object {
        private const val TAG = "徐"

        /**
         * 打印调试日志
         * @param message 日志消息
         */
        fun d(message: String) {
            Log.d(TAG, message)
        }

        /**
         * 打印信息日志
         * @param message 日志消息
         */
        fun i(message: String) {
            Log.i(TAG, message)
        }

        /**
         * 打印警告日志
         * @param message 日志消息
         */
        fun w(message: String) {
            Log.w(TAG, message)
        }

        /**
         * 打印错误日志
         * @param message 日志消息
         */
        fun e(message: String) {
            Log.e(TAG, message)
        }

        /**
         * 打印详细日志
         * @param message 日志消息
         */
        fun v(message: String) {
            Log.v(TAG, message)
        }

        /**
         * 默认日志打印方法，使用info级别
         * 支持直接调用 XuLog("打印日志")
         * @param message 日志消息
         */
        operator fun invoke(message: String) {
            i(message)
        }
    }
}