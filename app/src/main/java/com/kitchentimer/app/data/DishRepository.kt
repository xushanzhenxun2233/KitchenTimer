package com.kitchentimer.app.data

import android.content.Context
import android.os.SystemClock
import com.kitchentimer.app.model.ActiveDishTimer
import com.kitchentimer.app.model.DishPreset
import com.kitchentimer.app.model.DishStep
import com.kitchentimer.app.model.TimerStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 菜谱与正在运行计时的本地持久化仓库（JSON 文件存储，支持后台被杀后恢复）
 */
class DishRepository(context: Context) {

    private val storageFile = File(context.filesDir, "dish_presets.json")
    private val activeTimersFile = File(context.filesDir, "active_timers.json")
    private val backupTimersFile = File(context.filesDir, "active_timers_backup.json")

    fun loadPresets(): List<DishPreset> {
        if (!storageFile.exists()) {
            val defaults = defaultPresets()
            savePresets(defaults)
            return defaults
        }
        return try {
            val text = storageFile.readText(Charsets.UTF_8)
            val array = JSONArray(text)
            val result = mutableListOf<DishPreset>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val steps = parseSteps(obj.optJSONArray("steps"))
                if (steps.isNotEmpty()) {
                    result.add(
                        DishPreset(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            icon = obj.optString("icon", "🍲"),
                            note = obj.optString("note", ""),
                            steps = steps
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            val defaults = defaultPresets()
            savePresets(defaults)
            defaults
        }
    }

    fun savePresets(presets: List<DishPreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("icon", preset.icon)
                put("note", preset.note)
                put("steps", serializeSteps(preset.steps))
            }
            array.put(obj)
        }
        storageFile.writeText(array.toString(2), Charsets.UTF_8)
    }

    /**
     * 持久化当前正在烹饪的计时器列表；非空时同步写入一份备份，便于手动恢复
     */
    fun saveActiveTimers(timers: List<ActiveDishTimer>) {
        val json = serializeActiveTimers(timers)
        activeTimersFile.writeText(json, Charsets.UTF_8)
        if (timers.isNotEmpty()) {
            backupTimersFile.writeText(json, Charsets.UTF_8)
        }
    }

    /**
     * 启动时恢复后台被杀前的活跃计时器，并根据真实流逝的系统时间重新校准剩余时长
     */
    fun loadActiveTimers(): List<ActiveDishTimer> {
        return loadTimersFromFile(activeTimersFile)
    }

    /**
     * 手动从最近一次非空备份中恢复计时器
     */
    fun loadBackupTimers(): List<ActiveDishTimer> {
        return loadTimersFromFile(backupTimersFile)
    }

    fun hasBackupTimers(): Boolean {
        return backupTimersFile.exists() && backupTimersFile.length() > 4
    }

    private fun loadTimersFromFile(file: File): List<ActiveDishTimer> {
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText(Charsets.UTF_8)
            val array = JSONArray(text)
            val nowWall = System.currentTimeMillis()
            val nowRealtime = SystemClock.elapsedRealtime()
            val result = mutableListOf<ActiveDishTimer>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val steps = parseSteps(obj.optJSONArray("steps"))
                if (steps.isEmpty()) continue

                val stepIndex = obj.optInt("currentStepIndex", 0).coerceIn(0, steps.lastIndex)
                val savedStatus = runCatching {
                    TimerStatus.valueOf(obj.optString("status", TimerStatus.PAUSED.name))
                }.getOrDefault(TimerStatus.PAUSED)
                val savedRemaining = obj.optLong("remainingMillis", 0L).coerceAtLeast(0L)
                val savedEndWall = obj.optLong("endWallClockMillis", 0L)

                val (restoredStatus, restoredRemaining, restoredEndRealtime, restoredEndWall) =
                    when (savedStatus) {
                        TimerStatus.RUNNING -> {
                            val remaining = if (savedEndWall > 0L) {
                                (savedEndWall - nowWall).coerceAtLeast(0L)
                            } else {
                                savedRemaining
                            }
                            if (remaining == 0L) {
                                Quad(TimerStatus.RINGING, 0L, 0L, 0L)
                            } else {
                                Quad(
                                    TimerStatus.RUNNING,
                                    remaining,
                                    nowRealtime + remaining,
                                    nowWall + remaining
                                )
                            }
                        }
                        else -> Quad(savedStatus, savedRemaining, 0L, 0L)
                    }

                result.add(
                    ActiveDishTimer(
                        instanceId = obj.optString("instanceId"),
                        dishId = obj.optString("dishId"),
                        dishName = obj.optString("dishName"),
                        icon = obj.optString("icon", "🍲"),
                        steps = steps,
                        currentStepIndex = stepIndex,
                        remainingMillis = restoredRemaining,
                        status = restoredStatus,
                        endRealtimeMillis = restoredEndRealtime,
                        endWallClockMillis = restoredEndWall
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeActiveTimers(timers: List<ActiveDishTimer>): String {
        val array = JSONArray()
        timers.forEach { timer ->
            val obj = JSONObject().apply {
                put("instanceId", timer.instanceId)
                put("dishId", timer.dishId)
                put("dishName", timer.dishName)
                put("icon", timer.icon)
                put("steps", serializeSteps(timer.steps))
                put("currentStepIndex", timer.currentStepIndex)
                put("remainingMillis", timer.remainingMillis)
                put("status", timer.status.name)
                put("endWallClockMillis", timer.endWallClockMillis)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    private fun parseSteps(stepsArray: JSONArray?): List<DishStep> {
        if (stepsArray == null) return emptyList()
        val steps = mutableListOf<DishStep>()
        for (j in 0 until stepsArray.length()) {
            val stepObj = stepsArray.getJSONObject(j)
            steps.add(
                DishStep(
                    id = stepObj.optString("id"),
                    name = stepObj.optString("name"),
                    durationSeconds = stepObj.optInt("durationSeconds", 60).coerceAtLeast(1)
                )
            )
        }
        return steps
    }

    private fun serializeSteps(steps: List<DishStep>): JSONArray {
        val stepsArray = JSONArray()
        steps.forEach { step ->
            stepsArray.put(
                JSONObject().apply {
                    put("id", step.id)
                    put("name", step.name)
                    put("durationSeconds", step.durationSeconds)
                }
            )
        }
        return stepsArray
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun defaultPresets(): List<DishPreset> = listOf(
        DishPreset(
            id = "preset_soft_boiled_egg",
            name = "溏心蛋",
            icon = "🥚",
            note = "蛋白嫩滑、蛋黄流心",
            steps = listOf(
                DishStep(id = "egg_1", name = "大火烧水至沸腾", durationSeconds = 3 * 60),
                DishStep(id = "egg_2", name = "下蛋中火煮制", durationSeconds = 6 * 60 + 30),
                DishStep(id = "egg_3", name = "捞出过冰水降温", durationSeconds = 2 * 60)
            )
        ),
        DishPreset(
            id = "preset_braised_pork",
            name = "红烧肉",
            icon = "🥩",
            note = "软糯入味、肥而不腻",
            steps = listOf(
                DishStep(id = "pork_1", name = "冷水下锅焯水去沫", durationSeconds = 5 * 60),
                DishStep(id = "pork_2", name = "炒糖色煸炒上色", durationSeconds = 4 * 60),
                DishStep(id = "pork_3", name = "加热水小火慢炖", durationSeconds = 45 * 60),
                DishStep(id = "pork_4", name = "开盖大火收浓汤汁", durationSeconds = 5 * 60)
            )
        ),
        DishPreset(
            id = "preset_steamed_fish",
            name = "清蒸鲈鱼",
            icon = "🐟",
            note = "水开上锅，鲜嫩不老",
            steps = listOf(
                DishStep(id = "fish_1", name = "水开上锅大火蒸", durationSeconds = 8 * 60),
                DishStep(id = "fish_2", name = "关火焖蒸虚蒸", durationSeconds = 2 * 60)
            )
        ),
        DishPreset(
            id = "preset_pour_over_coffee",
            name = "手冲咖啡",
            icon = "☕",
            note = "三段式冲煮，92℃水温",
            steps = listOf(
                DishStep(id = "coffee_1", name = "注水闷蒸排气", durationSeconds = 30),
                DishStep(id = "coffee_2", name = "第一段绕圈注水", durationSeconds = 45),
                DishStep(id = "coffee_3", name = "第二段中心注水", durationSeconds = 45),
                DishStep(id = "coffee_4", name = "等待滴滤完成", durationSeconds = 30)
            )
        )
    )
}
