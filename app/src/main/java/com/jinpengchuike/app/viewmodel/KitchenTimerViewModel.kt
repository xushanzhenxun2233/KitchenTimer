package com.jinpengchuike.app.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinpengchuike.app.audio.AlarmSoundManager
import com.jinpengchuike.app.data.DishRepository
import com.jinpengchuike.app.model.ActiveDishTimer
import com.jinpengchuike.app.model.DishPreset
import com.jinpengchuike.app.model.TimerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KitchenTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DishRepository(application)
    private val alarmManager = AlarmSoundManager(application)

    private val _presets = MutableStateFlow<List<DishPreset>>(emptyList())
    val presets: StateFlow<List<DishPreset>> = _presets.asStateFlow()

    private val _activeTimers = MutableStateFlow<List<ActiveDishTimer>>(emptyList())
    val activeTimers: StateFlow<List<ActiveDishTimer>> = _activeTimers.asStateFlow()

    /** 默认开启屏幕常亮 */
    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    /** 后台被杀重启后自动恢复的任务数量提示（>0 时在界面顶部提示） */
    private val _restoredNoticeCount = MutableStateFlow(0)
    val restoredNoticeCount: StateFlow<Int> = _restoredNoticeCount.asStateFlow()

    /** 是否存在可手动恢复的上次计时记录 */
    private val _hasBackup = MutableStateFlow(false)
    val hasBackup: StateFlow<Boolean> = _hasBackup.asStateFlow()

    init {
        _presets.value = repository.loadPresets()
        val restored = repository.loadActiveTimers()
        if (restored.isNotEmpty()) {
            _activeTimers.value = restored
            _restoredNoticeCount.value = restored.size
            repository.saveActiveTimers(restored)
            syncAlarmState()
        }
        _hasBackup.value = repository.hasBackupTimers()
        startTickerLoop()
    }

    private fun startTickerLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(200L)
                tickTimers()
            }
        }
    }

    private fun tickTimers() {
        val currentList = _activeTimers.value
        if (currentList.none { it.status == TimerStatus.RUNNING }) return

        val now = SystemClock.elapsedRealtime()
        var newlyRinging = false

        val updated = currentList.map { timer ->
            if (timer.status == TimerStatus.RUNNING) {
                val remaining = (timer.endRealtimeMillis - now).coerceAtLeast(0L)
                if (remaining == 0L) {
                    newlyRinging = true
                    timer.copy(
                        remainingMillis = 0L,
                        status = TimerStatus.RINGING,
                        endRealtimeMillis = 0L,
                        endWallClockMillis = 0L
                    )
                } else {
                    timer.copy(remainingMillis = remaining)
                }
            } else {
                timer
            }
        }

        _activeTimers.value = updated
        if (newlyRinging) {
            persistSnapshot()
            alarmManager.startAlarm()
        }
    }

    fun dismissRestoredNotice() {
        _restoredNoticeCount.value = 0
    }

    /**
     * 立即将当前所有计时器状态写入本地文件（供 onStop 或关键操作后调用）
     */
    fun persistSnapshot() {
        val list = _activeTimers.value
        repository.saveActiveTimers(list)
        _hasBackup.value = repository.hasBackupTimers()
    }

    /**
     * 手动恢复最近一次保存的计时记录
     */
    fun restoreLastBackup() {
        val backup = repository.loadBackupTimers()
        if (backup.isNotEmpty()) {
            _activeTimers.value = backup
            _restoredNoticeCount.value = backup.size
            persistSnapshot()
            syncAlarmState()
        }
    }

    fun toggleKeepScreenOn() {
        _keepScreenOn.update { !it }
    }

    // ==================== 菜谱预设管理 ====================

    fun savePreset(preset: DishPreset) {
        _presets.update { list ->
            val exists = list.any { it.id == preset.id }
            val next = if (exists) {
                list.map { if (it.id == preset.id) preset else it }
            } else {
                list + preset
            }
            repository.savePresets(next)
            next
        }
    }

    fun deletePreset(presetId: String) {
        _presets.update { list ->
            val next = list.filterNot { it.id == presetId }
            repository.savePresets(next)
            next
        }
    }

    fun restoreDefaultPresets() {
        val defaults = repository.defaultPresets()
        _presets.update { current ->
            val existingIds = current.map { it.id }.toSet()
            val merged = current + defaults.filterNot { it.id in existingIds }
            repository.savePresets(merged)
            merged
        }
    }

    // ==================== 多道菜同时计时控制 ====================

    /**
     * 启动一道菜的计时（可同时开启多道菜，同一道菜也可开启多份）
     */
    fun startDish(preset: DishPreset): String {
        val firstStep = preset.steps.firstOrNull() ?: return ""
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val durationMillis = firstStep.durationSeconds * 1000L
        val timer = ActiveDishTimer(
            dishId = preset.id,
            dishName = preset.name,
            icon = preset.icon,
            steps = preset.steps,
            currentStepIndex = 0,
            remainingMillis = durationMillis,
            status = TimerStatus.RUNNING,
            endRealtimeMillis = nowRealtime + durationMillis,
            endWallClockMillis = nowWall + durationMillis
        )
        _activeTimers.update { list -> listOf(timer) + list }
        persistSnapshot()
        return timer.instanceId
    }

    fun togglePauseResume(instanceId: String) {
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.instanceId != instanceId) return@map timer
                when (timer.status) {
                    TimerStatus.RUNNING -> {
                        val remaining = (timer.endRealtimeMillis - nowRealtime).coerceAtLeast(0L)
                        timer.copy(
                            remainingMillis = remaining,
                            status = TimerStatus.PAUSED,
                            endRealtimeMillis = 0L,
                            endWallClockMillis = 0L
                        )
                    }
                    TimerStatus.PAUSED -> {
                        timer.copy(
                            status = TimerStatus.RUNNING,
                            endRealtimeMillis = nowRealtime + timer.remainingMillis,
                            endWallClockMillis = nowWall + timer.remainingMillis
                        )
                    }
                    else -> timer
                }
            }
        }
        persistSnapshot()
    }

    /**
     * 延长当前流程时间（例如临时加 1 分钟）
     */
    fun addExtraSeconds(instanceId: String, extraSeconds: Int) {
        val extraMillis = extraSeconds * 1000L
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.instanceId != instanceId) return@map timer
                when (timer.status) {
                    TimerStatus.RUNNING -> {
                        val nextRemaining = (timer.endRealtimeMillis - nowRealtime).coerceAtLeast(0L) + extraMillis
                        timer.copy(
                            remainingMillis = nextRemaining,
                            endRealtimeMillis = nowRealtime + nextRemaining,
                            endWallClockMillis = nowWall + nextRemaining
                        )
                    }
                    TimerStatus.PAUSED -> timer.copy(
                        remainingMillis = timer.remainingMillis + extraMillis
                    )
                    TimerStatus.RINGING -> timer.copy(
                        remainingMillis = extraMillis,
                        status = TimerStatus.RUNNING,
                        endRealtimeMillis = nowRealtime + extraMillis,
                        endWallClockMillis = nowWall + extraMillis
                    )
                    else -> timer
                }
            }
        }
        persistSnapshot()
        syncAlarmState()
    }

    /**
     * 修改当前流程的剩余时间（单位：秒，至少 1 秒）
     */
    fun updateCurrentStepRemainingSeconds(instanceId: String, newSeconds: Int) {
        val safeMillis = newSeconds.coerceAtLeast(1) * 1000L
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.instanceId != instanceId) return@map timer
                when (timer.status) {
                    TimerStatus.PAUSED -> timer.copy(
                        remainingMillis = safeMillis,
                        endRealtimeMillis = 0L,
                        endWallClockMillis = 0L
                    )
                    TimerStatus.RUNNING, TimerStatus.RINGING, TimerStatus.WAITING_NEXT -> timer.copy(
                        remainingMillis = safeMillis,
                        status = TimerStatus.RUNNING,
                        endRealtimeMillis = nowRealtime + safeMillis,
                        endWallClockMillis = nowWall + safeMillis
                    )
                    TimerStatus.COMPLETED -> timer
                }
            }
        }
        persistSnapshot()
        syncAlarmState()
    }

    /**
     * 仅停止该道菜的响铃：若有下一步进入 WAITING_NEXT，若已是最后一步进入 COMPLETED
     */
    fun silenceRinging(instanceId: String) {
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.instanceId != instanceId || timer.status != TimerStatus.RINGING) {
                    return@map timer
                }
                if (timer.hasNextStep) {
                    timer.copy(status = TimerStatus.WAITING_NEXT, endRealtimeMillis = 0L, endWallClockMillis = 0L)
                } else {
                    timer.copy(status = TimerStatus.COMPLETED, endRealtimeMillis = 0L, endWallClockMillis = 0L)
                }
            }
        }
        persistSnapshot()
        syncAlarmState()
    }

    /**
     * 手动确认并开始下一个流程（若是最后一步则标记完成）
     */
    fun confirmAndStartNextStep(instanceId: String) {
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.instanceId != instanceId) return@map timer
                val nextIndex = timer.currentStepIndex + 1
                val nextStep = timer.steps.getOrNull(nextIndex)
                if (nextStep != null) {
                    val durationMillis = nextStep.durationSeconds * 1000L
                    timer.copy(
                        currentStepIndex = nextIndex,
                        remainingMillis = durationMillis,
                        status = TimerStatus.RUNNING,
                        endRealtimeMillis = nowRealtime + durationMillis,
                        endWallClockMillis = nowWall + durationMillis
                    )
                } else {
                    timer.copy(
                        remainingMillis = 0L,
                        status = TimerStatus.COMPLETED,
                        endRealtimeMillis = 0L,
                        endWallClockMillis = 0L
                    )
                }
            }
        }
        persistSnapshot()
        syncAlarmState()
    }

    /**
     * 重新从第一个流程开始烹饪这道菜
     */
    fun restartDish(instanceId: String) {
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.instanceId != instanceId) return@map timer
                val firstStep = timer.steps.firstOrNull() ?: return@map timer
                val durationMillis = firstStep.durationSeconds * 1000L
                timer.copy(
                    currentStepIndex = 0,
                    remainingMillis = durationMillis,
                    status = TimerStatus.RUNNING,
                    endRealtimeMillis = nowRealtime + durationMillis,
                    endWallClockMillis = nowWall + durationMillis
                )
            }
        }
        persistSnapshot()
        syncAlarmState()
    }

    /**
     * 从烹饪工作台移除该计时任务
     */
    fun removeActiveTimer(instanceId: String) {
        _activeTimers.update { list -> list.filterNot { it.instanceId == instanceId } }
        persistSnapshot()
        syncAlarmState()
    }

    /**
     * 一键静音所有正在响铃的菜（全部转为 WAITING_NEXT 或 COMPLETED）
     */
    fun silenceAllRinging() {
        _activeTimers.update { list ->
            list.map { timer ->
                if (timer.status != TimerStatus.RINGING) {
                    timer
                } else if (timer.hasNextStep) {
                    timer.copy(status = TimerStatus.WAITING_NEXT, endRealtimeMillis = 0L, endWallClockMillis = 0L)
                } else {
                    timer.copy(status = TimerStatus.COMPLETED, endRealtimeMillis = 0L, endWallClockMillis = 0L)
                }
            }
        }
        persistSnapshot()
        alarmManager.stopAlarm()
    }

    /**
     * 根据当前是否还有任意菜处于 RINGING 状态，同步响铃开关
     */
    private fun syncAlarmState() {
        val anyRinging = _activeTimers.value.any { it.status == TimerStatus.RINGING }
        if (anyRinging) {
            alarmManager.startAlarm()
        } else {
            alarmManager.stopAlarm()
        }
    }

    override fun onCleared() {
        persistSnapshot()
        super.onCleared()
        alarmManager.release()
    }
}
