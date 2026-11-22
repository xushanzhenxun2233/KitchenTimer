package com.jinpengchuike.app.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinpengchuike.app.model.ActiveDishTimer
import com.jinpengchuike.app.model.DishPreset
import com.jinpengchuike.app.model.TimerStatus
import com.jinpengchuike.app.model.formatCountdownMillis
import com.jinpengchuike.app.model.formatDurationSeconds
import com.jinpengchuike.app.ui.theme.AlarmRed
import com.jinpengchuike.app.ui.theme.SuccessGreen
import com.jinpengchuike.app.ui.theme.WarmOrange

@Composable
fun CookingScreen(
    activeTimers: List<ActiveDishTimer>,
    presets: List<DishPreset>,
    restoredNoticeCount: Int,
    hasBackup: Boolean,
    onDismissRestoredNotice: () -> Unit,
    onRestoreLastBackup: () -> Unit,
    onStartPreset: (DishPreset) -> Unit,
    onTogglePauseResume: (String) -> Unit,
    onAddExtraSeconds: (String, Int) -> Unit,
    onUpdateRemainingSeconds: (String, Int) -> Unit,
    onSilenceRinging: (String) -> Unit,
    onConfirmNextStep: (String) -> Unit,
    onRestartDish: (String) -> Unit,
    onRemoveTimer: (String) -> Unit,
    onSilenceAll: () -> Unit,
    onGoToPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringingCount = activeTimers.count { it.status == TimerStatus.RINGING }
    var editingTimeTimer by remember { mutableStateOf<ActiveDishTimer?>(null) }

    // 将正在响铃的任务排在最前面，方便第一时间操作
    val sortedTimers = activeTimers.sortedBy { timer ->
        when (timer.status) {
            TimerStatus.RINGING -> 0
            TimerStatus.WAITING_NEXT -> 1
            TimerStatus.RUNNING -> 2
            TimerStatus.PAUSED -> 3
            TimerStatus.COMPLETED -> 4
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (ringingCount > 0) {
            RingingBanner(
                ringingCount = ringingCount,
                onSilenceAll = onSilenceAll
            )
        }

        if (restoredNoticeCount > 0) {
            RestoredNoticeBanner(
                count = restoredNoticeCount,
                onDismiss = onDismissRestoredNotice
            )
        }

        if (sortedTimers.isEmpty()) {
            EmptyCookingState(
                presets = presets,
                hasBackup = hasBackup,
                onRestoreLastBackup = onRestoreLastBackup,
                onStartPreset = onStartPreset,
                onGoToPresets = onGoToPresets
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sortedTimers, key = { it.instanceId }) { timer ->
                    ActiveDishTimerCard(
                        timer = timer,
                        onTogglePauseResume = { onTogglePauseResume(timer.instanceId) },
                        onAddExtraMinute = { onAddExtraSeconds(timer.instanceId, 60) },
                        onOpenEditTime = { editingTimeTimer = timer },
                        onSilenceRinging = { onSilenceRinging(timer.instanceId) },
                        onConfirmNextStep = { onConfirmNextStep(timer.instanceId) },
                        onRestartDish = { onRestartDish(timer.instanceId) },
                        onRemove = { onRemoveTimer(timer.instanceId) }
                    )
                }

                item {
                    SafeConfirmOutlinedButton(
                        onConfirm = onGoToPresets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { armed ->
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (armed) "再点进入菜谱" else "再开一道菜同时计时")
                    }
                }
            }
        }
    }

    editingTimeTimer?.let { target ->
        EditRemainingTimeDialog(
            timer = target,
            onDismiss = { editingTimeTimer = null },
            onConfirm = { newSeconds ->
                onUpdateRemainingSeconds(target.instanceId, newSeconds)
                editingTimeTimer = null
            }
        )
    }
}

@Composable
private fun RestoredNoticeBanner(
    count: Int,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "已自动恢复上次 $count 道菜的计时进度",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onDismiss) {
                Text("知道了", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RingingBanner(
    ringingCount: Int,
    onSilenceAll: () -> Unit
) {
    Surface(
        color = AlarmRed,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "有 $ringingCount 道菜时间到了！",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            SafeConfirmTextButton(
                onConfirm = onSilenceAll,
                normalContentColor = Color.White,
                armedContentColor = Color(0xFFFFE599)
            ) { armed ->
                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (armed) "再点静音" else "全部静音", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyCookingState(
    presets: List<DishPreset>,
    hasBackup: Boolean,
    onRestoreLastBackup: () -> Unit,
    onStartPreset: (DishPreset) -> Unit,
    onGoToPresets: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(20.dp))
            Text(text = "👨‍🍳", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "当前没有正在计时的菜",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "支持多道菜同时计时，所有操作均需轻点两次确认防误触",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            SafeConfirmButton(
                onConfirm = onGoToPresets,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) { armed ->
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (armed) "再点进入菜谱" else "去菜谱预设选择或新建菜品")
            }

            if (hasBackup) {
                Spacer(Modifier.height(10.dp))
                SafeConfirmOutlinedButton(
                    onConfirm = onRestoreLastBackup,
                    shape = RoundedCornerShape(14.dp)
                ) { armed ->
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (armed) "再点确认恢复" else "恢复上次计时记录")
                }
            }
        }

        if (presets.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "快速开始常用菜品",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(presets.take(4), key = { it.id }) { preset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = preset.icon, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${preset.steps.size}个流程 · 共 ${formatDurationSeconds(preset.totalDurationSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SafeConfirmTonalButton(
                            onConfirm = { onStartPreset(preset) },
                            shape = RoundedCornerShape(12.dp)
                        ) { armed ->
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (armed) "再点开始" else "开始")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDishTimerCard(
    timer: ActiveDishTimer,
    onTogglePauseResume: () -> Unit,
    onAddExtraMinute: () -> Unit,
    onOpenEditTime: () -> Unit,
    onSilenceRinging: () -> Unit,
    onConfirmNextStep: () -> Unit,
    onRestartDish: () -> Unit,
    onRemove: () -> Unit
) {
    val isRinging = timer.status == TimerStatus.RINGING
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulsingBorderColor by infiniteTransition.animateColor(
        initialValue = AlarmRed,
        targetValue = WarmOrange,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_color"
    )

    val borderStroke = when (timer.status) {
        TimerStatus.RINGING -> BorderStroke(3.dp, pulsingBorderColor)
        TimerStatus.WAITING_NEXT -> BorderStroke(2.dp, WarmOrange)
        TimerStatus.COMPLETED -> BorderStroke(1.5.dp, SuccessGreen)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    }

    val containerColor = when (timer.status) {
        TimerStatus.RINGING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRinging) 6.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部：菜名 + 状态徽章 + 防误触移除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = timer.icon, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = timer.dishName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusBadge(status = timer.status)
                    }
                }
                SafeConfirmTextButton(
                    onConfirm = onRemove,
                    normalContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    armedContentColor = AlarmRed
                ) { armed ->
                    Text(
                        text = if (armed) "再点移除" else "移除",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (armed) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 当前流程信息与超大倒计时数字
            if (timer.status == TimerStatus.COMPLETED) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉 全部流程已完成", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "共完成 ${timer.steps.size} 个烹饪步骤",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val currentStep = timer.currentStep
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "步骤 ${timer.currentStepIndex + 1} / ${timer.steps.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentStep?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatCountdownMillis(timer.remainingMillis),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = when (timer.status) {
                            TimerStatus.RINGING -> AlarmRed
                            TimerStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = {
                        if (timer.status == TimerStatus.RINGING || timer.status == TimerStatus.WAITING_NEXT) {
                            1f
                        } else {
                            timer.currentStepProgress
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isRinging) AlarmRed else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                val nextStepText = timer.nextStep?.let { next ->
                    "下一步：${next.name} (${formatDurationSeconds(next.durationSeconds)})"
                } ?: "这是最后一个流程"
                Text(
                    text = nextStepText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // 流程胶囊列表
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(timer.steps) { index, step ->
                    val isDone = timer.status == TimerStatus.COMPLETED || index < timer.currentStepIndex
                    val isCurrent = timer.status != TimerStatus.COMPLETED && index == timer.currentStepIndex
                    val bgColor = when {
                        isDone -> SuccessGreen.copy(alpha = 0.16f)
                        isCurrent && isRinging -> AlarmRed.copy(alpha = 0.2f)
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor = when {
                        isDone -> SuccessGreen
                        isCurrent && isRinging -> AlarmRed
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            text = "${index + 1}.${step.name} ${formatDurationSeconds(step.durationSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 底部控制按钮区（全部使用防误触二次确认 + 纯文字清晰标识）
            when (timer.status) {
                TimerStatus.RINGING -> {
                    val normalLabel = if (timer.hasNextStep) {
                        "停止响铃 · 下一步 (${timer.nextStep?.name})"
                    } else {
                        "停止响铃 · 完成烹饪"
                    }
                    val armedLabel = if (timer.hasNextStep) {
                        "再点确认 · 开始下一步"
                    } else {
                        "再点确认 · 完成烹饪"
                    }
                    SafeConfirmButton(
                        onConfirm = onConfirmNextStep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        normalColors = ButtonDefaults.buttonColors(containerColor = AlarmRed, contentColor = Color.White),
                        armedColors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D0208), contentColor = Color.White)
                    ) { armed ->
                        Icon(Icons.Default.NotificationsOff, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (armed) armedLabel else normalLabel,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SafeConfirmOutlinedButton(
                            onConfirm = onSilenceRinging,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) { armed ->
                            Text(if (armed) "再点静音" else "仅停响铃", maxLines = 1)
                        }
                        SafeConfirmOutlinedButton(
                            onConfirm = onAddExtraMinute,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) { armed ->
                            Text(if (armed) "再点+1分" else "+1分继续", maxLines = 1)
                        }
                        SafeConfirmOutlinedButton(
                            onConfirm = onOpenEditTime,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) { armed ->
                            Text(if (armed) "再点修改" else "改时间", maxLines = 1)
                        }
                    }
                }

                TimerStatus.WAITING_NEXT -> {
                    SafeConfirmButton(
                        onConfirm = onConfirmNextStep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { armed ->
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (armed) {
                                "再点确认开始下一步"
                            } else {
                                "开始下一步：${timer.nextStep?.name ?: ""}"
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                    val isRunning = timer.status == TimerStatus.RUNNING
                    val nextLabel = if (timer.hasNextStep) "下一步" else "完成"
                    val nextArmedLabel = if (timer.hasNextStep) "再点下一步" else "再点完成"

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SafeConfirmButton(
                                onConfirm = onTogglePauseResume,
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) { armed ->
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isRunning && armed -> "再点暂停"
                                        isRunning -> "暂停"
                                        armed -> "再点继续"
                                        else -> "继续"
                                    },
                                    maxLines = 1
                                )
                            }

                            SafeConfirmTonalButton(
                                onConfirm = onAddExtraMinute,
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) { armed ->
                                Text(if (armed) "再点+1分" else "+1分", maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SafeConfirmOutlinedButton(
                                onConfirm = onOpenEditTime,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) { armed ->
                                Text(if (armed) "再点改时间" else "改时间", maxLines = 1)
                            }

                            SafeConfirmOutlinedButton(
                                onConfirm = onConfirmNextStep,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) { armed ->
                                Text(if (armed) nextArmedLabel else nextLabel, maxLines = 1)
                            }
                        }
                    }
                }

                TimerStatus.COMPLETED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SafeConfirmTonalButton(
                            onConfirm = onRestartDish,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { armed ->
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (armed) "再点重做" else "再来一次")
                        }
                        SafeConfirmOutlinedButton(
                            onConfirm = onRemove,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { armed ->
                            Text(if (armed) "再点移除" else "完成移除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRemainingTimeDialog(
    timer: ActiveDishTimer,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val initialSeconds = ((timer.remainingMillis + 999L) / 1000L).toInt().coerceAtLeast(1)
    var hoursText by remember(timer.instanceId) { mutableStateOf((initialSeconds / 3600).toString()) }
    var minutesText by remember(timer.instanceId) { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }
    var secondsText by remember(timer.instanceId) { mutableStateOf((initialSeconds % 60).toString()) }

    fun currentTotalSeconds(): Int {
        val h = hoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val m = minutesText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val s = secondsText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return h * 3600 + m * 60 + s
    }

    fun adjustBy(deltaSeconds: Int) {
        val next = (currentTotalSeconds() + deltaSeconds).coerceAtLeast(1)
        hoursText = (next / 3600).toString()
        minutesText = ((next % 3600) / 60).toString()
        secondsText = (next % 60).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "修改当前步骤时间",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${timer.dishName} · ${timer.currentStep?.name ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("时") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("分") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("秒") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { adjustBy(-30) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("-30秒", style = MaterialTheme.typography.labelSmall) }
                    FilledTonalButton(
                        onClick = { adjustBy(30) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("+30秒", style = MaterialTheme.typography.labelSmall) }
                    FilledTonalButton(
                        onClick = { adjustBy(60) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("+1分", style = MaterialTheme.typography.labelSmall) }
                    FilledTonalButton(
                        onClick = { adjustBy(300) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("+5分", style = MaterialTheme.typography.labelSmall) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = currentTotalSeconds().coerceAtLeast(1)
                    onConfirm(total)
                }
            ) {
                Text("确定修改", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun StatusBadge(status: TimerStatus) {
    val (label, color) = when (status) {
        TimerStatus.RUNNING -> "计时中" to MaterialTheme.colorScheme.primary
        TimerStatus.PAUSED -> "已暂停" to MaterialTheme.colorScheme.onSurfaceVariant
        TimerStatus.RINGING -> "🔔 时间到！正在响铃" to AlarmRed
        TimerStatus.WAITING_NEXT -> "等待手动开始下一步" to WarmOrange
        TimerStatus.COMPLETED -> "已完成" to SuccessGreen
    }
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
