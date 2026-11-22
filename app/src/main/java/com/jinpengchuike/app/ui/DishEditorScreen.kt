package com.jinpengchuike.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinpengchuike.app.model.DISH_ICON_OPTIONS
import com.jinpengchuike.app.model.DishPreset
import com.jinpengchuike.app.model.DishStep
import com.jinpengchuike.app.model.formatDurationChinese
import java.util.UUID

private data class EditableStep(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val hours: String = "0",
    val minutes: String = "5",
    val seconds: String = "0"
) {
    fun toTotalSeconds(): Int {
        val h = hours.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val m = minutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val s = seconds.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return h * 3600 + m * 60 + s
    }

    fun addSeconds(delta: Int): EditableStep {
        val next = (toTotalSeconds() + delta).coerceAtLeast(0)
        val h = next / 3600
        val m = (next % 3600) / 60
        val s = next % 60
        return copy(hours = h.toString(), minutes = m.toString(), seconds = s.toString())
    }

    companion object {
        fun fromDishStep(step: DishStep): EditableStep {
            val h = step.durationSeconds / 3600
            val m = (step.durationSeconds % 3600) / 60
            val s = step.durationSeconds % 60
            return EditableStep(
                id = step.id,
                name = step.name,
                hours = h.toString(),
                minutes = m.toString(),
                seconds = s.toString()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishEditorScreen(
    initialPreset: DishPreset?,
    onSave: (DishPreset) -> Unit,
    onCancel: () -> Unit
) {
    var dishName by remember(initialPreset) { mutableStateOf(initialPreset?.name ?: "") }
    var dishNote by remember(initialPreset) { mutableStateOf(initialPreset?.note ?: "") }
    var selectedIcon by remember(initialPreset) { mutableStateOf(initialPreset?.icon ?: "🍲") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val editableSteps = remember(initialPreset) {
        mutableStateListOf<EditableStep>().apply {
            if (initialPreset != null && initialPreset.steps.isNotEmpty()) {
                addAll(initialPreset.steps.map { EditableStep.fromDishStep(it) })
            } else {
                add(EditableStep(name = "步骤 1", hours = "0", minutes = "5", seconds = "0"))
            }
        }
    }

    val totalSeconds = editableSteps.sumOf { it.toTotalSeconds() }

    fun handleSave() {
        val trimmedName = dishName.trim()
        if (trimmedName.isEmpty()) {
            errorMessage = "请输入菜品名称"
            return
        }
        if (editableSteps.isEmpty()) {
            errorMessage = "请至少添加一个流程"
            return
        }
        val builtSteps = mutableListOf<DishStep>()
        editableSteps.forEachIndexed { idx, item ->
            val secs = item.toTotalSeconds()
            if (secs <= 0) {
                errorMessage = "第 ${idx + 1} 个流程的计时时长必须大于 0 秒"
                return
            }
            val stepName = item.name.trim().ifEmpty { "流程 ${idx + 1}" }
            builtSteps.add(
                DishStep(
                    id = item.id,
                    name = stepName,
                    durationSeconds = secs
                )
            )
        }
        errorMessage = null
        val preset = DishPreset(
            id = initialPreset?.id ?: UUID.randomUUID().toString(),
            name = trimmedName,
            icon = selectedIcon,
            note = dishNote.trim(),
            steps = builtSteps
        )
        onSave(preset)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialPreset == null) "新建菜谱" else "编辑菜谱",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = ::handleSave) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 基本信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "菜品信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = dishName,
                            onValueChange = {
                                dishName = it
                                errorMessage = null
                            },
                            label = { Text("菜名 *") },
                            placeholder = { Text("例如：红烧排骨、水煮鸡胸肉") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = dishNote,
                            onValueChange = { dishNote = it },
                            label = { Text("备注（可选）") },
                            placeholder = { Text("例如：大火烧开后转小火") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "选择图标",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DISH_ICON_OPTIONS) { emoji ->
                                val isSelected = emoji == selectedIcon
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable { selectedIcon = emoji }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 流程区标题
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "烹饪流程（共 ${editableSteps.size} 步）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "总时长：${formatDurationChinese(totalSeconds)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 流程编辑列表（可自由增删、排序、改时长）
            itemsIndexed(editableSteps, key = { _, item -> item.id }) { index, step ->
                StepEditorCard(
                    index = index,
                    totalCount = editableSteps.size,
                    step = step,
                    onUpdate = { updated ->
                        editableSteps[index] = updated
                        errorMessage = null
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val current = editableSteps.removeAt(index)
                            editableSteps.add(index - 1, current)
                        }
                    },
                    onMoveDown = {
                        if (index < editableSteps.lastIndex) {
                            val current = editableSteps.removeAt(index)
                            editableSteps.add(index + 1, current)
                        }
                    },
                    onDelete = {
                        if (editableSteps.size > 1) {
                            editableSteps.removeAt(index)
                        }
                    }
                )
            }

            // 添加新流程按钮
            item {
                OutlinedButton(
                    onClick = {
                        editableSteps.add(
                            EditableStep(
                                name = "步骤 ${editableSteps.size + 1}",
                                hours = "0",
                                minutes = "3",
                                seconds = "0"
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加下一个流程", fontWeight = FontWeight.Bold)
                }
            }

            // 底部保存按钮
            item {
                Button(
                    onClick = ::handleSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存菜谱", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepEditorCard(
    index: Int,
    totalCount: Int,
    step: EditableStep,
    onUpdate: (EditableStep) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "第 ${index + 1} 步 · ${formatDurationChinese(step.toTotalSeconds())}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "上移", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = index < totalCount - 1, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "下移", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, enabled = totalCount > 1, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "删除此流程",
                            modifier = Modifier.size(18.dp),
                            tint = if (totalCount > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = step.name,
                onValueChange = { onUpdate(step.copy(name = it)) },
                label = { Text("流程名称") },
                placeholder = { Text("例如：焯水、小火慢炖、大火收汁") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 时 / 分 / 秒 输入框
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = step.hours,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(2)
                        onUpdate(step.copy(hours = digits))
                    },
                    label = { Text("时") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = step.minutes,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(3)
                        onUpdate(step.copy(minutes = digits))
                    },
                    label = { Text("分") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = step.seconds,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(2)
                        onUpdate(step.copy(seconds = digits))
                    },
                    label = { Text("秒") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // 快捷加时按钮，方便单手快速设定时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickTimeChip("+10秒", Modifier.weight(1f)) { onUpdate(step.addSeconds(10)) }
                QuickTimeChip("+30秒", Modifier.weight(1f)) { onUpdate(step.addSeconds(30)) }
                QuickTimeChip("+1分", Modifier.weight(1f)) { onUpdate(step.addSeconds(60)) }
                QuickTimeChip("+5分", Modifier.weight(1f)) { onUpdate(step.addSeconds(300)) }
                QuickTimeChip("清零", Modifier.weight(0.9f)) {
                    onUpdate(step.copy(hours = "0", minutes = "0", seconds = "0"))
                }
            }
        }
    }
}

@Composable
private fun QuickTimeChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
