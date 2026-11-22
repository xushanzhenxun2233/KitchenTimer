package com.kitchentimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitchentimer.app.model.DishPreset
import com.kitchentimer.app.model.formatDurationChinese
import com.kitchentimer.app.model.formatDurationSeconds
import com.kitchentimer.app.ui.theme.WarmOrange

@Composable
fun PresetsScreen(
    presets: List<DishPreset>,
    onStartDish: (DishPreset) -> Unit,
    onCreateDish: () -> Unit,
    onEditDish: (DishPreset) -> Unit,
    onDeleteDish: (String) -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<DishPreset?>(null) }
    val createDishConfirm = rememberSafeConfirm(onConfirm = onCreateDish)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = createDishConfirm.onTap,
                containerColor = if (createDishConfirm.armed) WarmOrange else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (createDishConfirm.armed) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (createDishConfirm.armed) "再点新建" else "新建菜谱") }
            )
        }
    ) { innerPadding ->
        if (presets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📖", fontSize = 52.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "还没有预设菜谱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点击右下角新建你的专属菜谱，自定义任意数量的流程与时长",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                SafeConfirmOutlinedButton(onConfirm = onRestoreDefaults) { armed ->
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (armed) "再点确认恢复" else "恢复内置示例菜谱")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    DishPresetCard(
                        preset = preset,
                        onStart = { onStartDish(preset) },
                        onEdit = { onEditDish(preset) },
                        onDelete = { pendingDelete = preset }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SafeConfirmTextButton(onConfirm = onRestoreDefaults) { armed ->
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (armed) "再点确认补充" else "补充内置示例菜谱")
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { dish ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除菜谱") },
            text = { Text("确定要删除「${dish.name}」吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDish(dish.id)
                        pendingDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DishPresetCard(
    preset: DishPreset,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = preset.icon, fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val summary = buildString {
                            append("${preset.steps.size}个流程 · 总计 ${formatDurationChinese(preset.totalDurationSeconds)}")
                            if (preset.note.isNotBlank()) {
                                append(" · ${preset.note}")
                            }
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SafeConfirmTextButton(
                        onConfirm = onEdit,
                        normalContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) { armed ->
                        Text(
                            text = if (armed) "再点编辑" else "编辑",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    SafeConfirmTextButton(
                        onConfirm = onDelete,
                        normalContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        armedContentColor = MaterialTheme.colorScheme.error
                    ) { armed ->
                        Text(
                            text = if (armed) "再点删除" else "删除",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 流程预览列表
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(preset.steps) { index, step ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${step.name}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatDurationSeconds(step.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SafeConfirmButton(
                onConfirm = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) { armed ->
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (armed) "再点确认开始计时" else "开始计时",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
