package com.jinpengchuike.app.model

import java.util.UUID

/**
 * 菜谱中的单个烹饪流程
 */
data class DishStep(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Int
)

/**
 * 预设菜谱
 */
data class DishPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "🍲",
    val note: String = "",
    val steps: List<DishStep>
) {
    val totalDurationSeconds: Int
        get() = steps.sumOf { it.durationSeconds }
}

/**
 * 单道正在烹饪的菜的计时状态
 */
enum class TimerStatus {
    /** 当前流程正在倒计时 */
    RUNNING,
    /** 当前流程已暂停 */
    PAUSED,
    /** 当前流程已到时，正在响铃，等待用户手动确认 */
    RINGING,
    /** 用户已停止响铃，等待手动点击开始下一步 */
    WAITING_NEXT,
    /** 所有流程均已完成 */
    COMPLETED
}

/**
 * 正在烹饪工作台上的单道菜运行实例（支持多道菜同时计时）
 */
data class ActiveDishTimer(
    val instanceId: String = UUID.randomUUID().toString(),
    val dishId: String,
    val dishName: String,
    val icon: String,
    val steps: List<DishStep>,
    val currentStepIndex: Int = 0,
    val remainingMillis: Long,
    val status: TimerStatus = TimerStatus.RUNNING,
    /** 仅在 status == RUNNING 时有效，记录基于 SystemClock.elapsedRealtime() 的目标结束时间点 */
    val endRealtimeMillis: Long = 0L,
    /** 仅在 status == RUNNING 时有效，记录基于 System.currentTimeMillis() 的目标结束时间点，用于后台被杀后恢复 */
    val endWallClockMillis: Long = 0L
) {
    val currentStep: DishStep?
        get() = steps.getOrNull(currentStepIndex)

    val nextStep: DishStep?
        get() = steps.getOrNull(currentStepIndex + 1)

    val hasNextStep: Boolean
        get() = currentStepIndex + 1 < steps.size

    val currentStepTotalMillis: Long
        get() = (currentStep?.durationSeconds ?: 0) * 1000L

    val currentStepProgress: Float
        get() {
            val total = currentStepTotalMillis
            if (total <= 0L) return 1f
            val elapsed = (total - remainingMillis).coerceIn(0L, total)
            return elapsed.toFloat() / total.toFloat()
        }
}

/**
 * 将毫秒格式化为倒计时显示（如 05:30 或 1:05:30）
 */
fun formatCountdownMillis(millis: Long): String {
    val totalSeconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
    return formatDurationSeconds(totalSeconds.toInt())
}

/**
 * 将秒数格式化为简洁时长（如 05:30 或 1:05:30）
 */
fun formatDurationSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * 将秒数格式化为中文易读时长（如 1小时5分、6分30秒、45秒）
 */
fun formatDurationChinese(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return buildString {
        if (hours > 0) append("${hours}小时")
        if (minutes > 0) append("${minutes}分")
        if (seconds > 0 || isEmpty()) append("${seconds}秒")
    }
}

val DISH_ICON_OPTIONS = listOf(
    "🍲", "🍳", "🥩", "🐟", "🍜", "🥚", "☕", "🥟",
    "🍗", "🦐", "🥦", "🍚", "🍰", "🫖", "🥘", "🔥"
)
