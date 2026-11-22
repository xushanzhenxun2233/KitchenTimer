package com.jinpengchuike.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jinpengchuike.app.ui.KitchenApp
import com.jinpengchuike.app.ui.theme.KitchenTimerTheme
import com.jinpengchuike.app.viewmodel.KitchenTimerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KitchenTimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 默认开启屏幕常亮，避免做菜时手机自动熄屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            KitchenTimerTheme {
                KitchenApp(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 切后台或即将被系统回收前，立即持久化当前计时进度
        viewModel.persistSnapshot()
    }
}
