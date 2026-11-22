package com.kitchentimer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitchentimer.app.model.DishPreset
import com.kitchentimer.app.model.TimerStatus
import com.kitchentimer.app.ui.theme.WarmOrange
import com.kitchentimer.app.viewmodel.KitchenTimerViewModel

private enum class MainTab {
    COOKING,
    PRESETS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenApp(viewModel: KitchenTimerViewModel) {
    val presets by viewModel.presets.collectAsState()
    val activeTimers by viewModel.activeTimers.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val restoredNoticeCount by viewModel.restoredNoticeCount.collectAsState()
    val hasBackup by viewModel.hasBackup.collectAsState()

    var currentTab by remember { mutableStateOf(MainTab.COOKING) }
    var isEditingDish by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<DishPreset?>(null) }

    if (isEditingDish) {
        DishEditorScreen(
            initialPreset = editingPreset,
            onSave = { saved ->
                viewModel.savePreset(saved)
                isEditingDish = false
                editingPreset = null
            },
            onCancel = {
                isEditingDish = false
                editingPreset = null
            }
        )
        return
    }

    val runningOrRingingCount = activeTimers.count { it.status != TimerStatus.COMPLETED }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            MainTab.COOKING -> "正在烹饪"
                            MainTab.PRESETS -> "菜谱预设"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    KeepScreenOnPill(
                        enabled = keepScreenOn,
                        onToggle = { viewModel.toggleKeepScreenOn() }
                    )
                    Spacer(Modifier.width(12.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == MainTab.COOKING,
                    onClick = { currentTab = MainTab.COOKING },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (runningOrRingingCount > 0) {
                                    Badge { Text(runningOrRingingCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = "正在烹饪")
                        }
                    },
                    label = { Text("正在烹饪") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.PRESETS,
                    onClick = { currentTab = MainTab.PRESETS },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "菜谱预设") },
                    label = { Text("菜谱预设") }
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            MainTab.COOKING -> {
                CookingScreen(
                    activeTimers = activeTimers,
                    presets = presets,
                    restoredNoticeCount = restoredNoticeCount,
                    hasBackup = hasBackup,
                    onDismissRestoredNotice = { viewModel.dismissRestoredNotice() },
                    onRestoreLastBackup = { viewModel.restoreLastBackup() },
                    onStartPreset = { preset -> viewModel.startDish(preset) },
                    onTogglePauseResume = { viewModel.togglePauseResume(it) },
                    onAddExtraSeconds = { id, sec -> viewModel.addExtraSeconds(id, sec) },
                    onUpdateRemainingSeconds = { id, sec -> viewModel.updateCurrentStepRemainingSeconds(id, sec) },
                    onSilenceRinging = { viewModel.silenceRinging(it) },
                    onConfirmNextStep = { viewModel.confirmAndStartNextStep(it) },
                    onRestartDish = { viewModel.restartDish(it) },
                    onRemoveTimer = { viewModel.removeActiveTimer(it) },
                    onSilenceAll = { viewModel.silenceAllRinging() },
                    onGoToPresets = { currentTab = MainTab.PRESETS },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainTab.PRESETS -> {
                PresetsScreen(
                    presets = presets,
                    onStartDish = { preset ->
                        viewModel.startDish(preset)
                        currentTab = MainTab.COOKING
                    },
                    onCreateDish = {
                        editingPreset = null
                        isEditingDish = true
                    },
                    onEditDish = { preset ->
                        editingPreset = preset
                        isEditingDish = true
                    },
                    onDeleteDish = { viewModel.deletePreset(it) },
                    onRestoreDefaults = { viewModel.restoreDefaultPresets() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOnPill(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val confirm = rememberSafeConfirm(onConfirm = onToggle)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            confirm.armed -> WarmOrange
            enabled -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.clickable(onClick = confirm.onTap)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.BrightnessHigh else Icons.Default.BrightnessLow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    confirm.armed -> Color.White
                    enabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = when {
                    confirm.armed && enabled -> "再点关常亮"
                    confirm.armed -> "再点开常亮"
                    enabled -> "屏幕常亮中"
                    else -> "常亮已关"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    confirm.armed -> Color.White
                    enabled -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
