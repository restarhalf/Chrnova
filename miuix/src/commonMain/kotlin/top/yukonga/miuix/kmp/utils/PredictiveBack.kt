package top.yukonga.miuix.kmp.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * 预测性返回手势的全局开关。
 *
 * App 有「设置 → 全局预测性返回动画」开关（默认开），关闭后返回键仍然被拦截、
 * 弹窗仍然关闭，只是不再驱动「滑到一半跟手」的动画。
 * App 侧在自己的根上提供 [LocalPredictiveBackEnabled]，值来自它的偏好存储。
 */
val LocalPredictiveBackEnabled = compositionLocalOf { PredictiveBackEnabledDefault }

/** 未注入时的默认行为：开启预测性返回动画。 */
const val PredictiveBackEnabledDefault: Boolean = true