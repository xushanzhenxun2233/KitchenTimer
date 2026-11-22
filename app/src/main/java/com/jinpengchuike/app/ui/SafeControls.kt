package com.jinpengchuike.app.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jinpengchuike.app.ui.theme.WarmOrange
import kotlinx.coroutines.delay

/**
 * 通用防误触状态管理：
 * - 第 1 次点击：进入待确认态（armed = true）
 * - 250ms 内忽略手抖误连点
 * - 250ms ~ 2500ms 内第 2 次点击：触发真正操作并复位
 * - 超过 2500ms 未确认：自动复位，防止误触
 */
class SafeConfirmController(
    val armed: Boolean,
    val onTap: () -> Unit
)

@Composable
fun rememberSafeConfirm(
    timeoutMillis: Long = 2500L,
    minGapMillis: Long = 250L,
    onConfirm: () -> Unit
): SafeConfirmController {
    var armed by remember { mutableStateOf(false) }
    var armedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(armed, armedAt) {
        if (armed) {
            delay(timeoutMillis)
            armed = false
        }
    }

    return SafeConfirmController(
        armed = armed,
        onTap = {
            val now = SystemClock.elapsedRealtime()
            if (!armed) {
                armed = true
                armedAt = now
            } else if (now - armedAt >= minGapMillis) {
                armed = false
                onConfirm()
            }
        }
    )
}

@Composable
fun SafeConfirmButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    normalColors: ButtonColors = ButtonDefaults.buttonColors(),
    armedColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = WarmOrange,
        contentColor = Color.White
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.(armed: Boolean) -> Unit
) {
    val controller = rememberSafeConfirm(onConfirm = onConfirm)
    Button(
        onClick = controller.onTap,
        modifier = modifier,
        shape = shape,
        colors = if (controller.armed) armedColors else normalColors,
        contentPadding = contentPadding
    ) {
        content(controller.armed)
    }
}

@Composable
fun SafeConfirmTonalButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.(armed: Boolean) -> Unit
) {
    val controller = rememberSafeConfirm(onConfirm = onConfirm)
    FilledTonalButton(
        onClick = controller.onTap,
        modifier = modifier,
        shape = shape,
        colors = if (controller.armed) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = WarmOrange,
                contentColor = Color.White
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        contentPadding = contentPadding
    ) {
        content(controller.armed)
    }
}

@Composable
fun SafeConfirmOutlinedButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.(armed: Boolean) -> Unit
) {
    val controller = rememberSafeConfirm(onConfirm = onConfirm)
    OutlinedButton(
        onClick = controller.onTap,
        modifier = modifier,
        shape = shape,
        border = if (controller.armed) {
            BorderStroke(2.dp, WarmOrange)
        } else {
            ButtonDefaults.outlinedButtonBorder(enabled = true)
        },
        colors = if (controller.armed) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = WarmOrange.copy(alpha = 0.14f),
                contentColor = WarmOrange
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        contentPadding = contentPadding
    ) {
        content(controller.armed)
    }
}

@Composable
fun SafeConfirmTextButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    normalContentColor: Color = MaterialTheme.colorScheme.primary,
    armedContentColor: Color = WarmOrange,
    content: @Composable RowScope.(armed: Boolean) -> Unit
) {
    val controller = rememberSafeConfirm(onConfirm = onConfirm)
    TextButton(
        onClick = controller.onTap,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (controller.armed) armedContentColor else normalContentColor
        )
    ) {
        content(controller.armed)
    }
}
