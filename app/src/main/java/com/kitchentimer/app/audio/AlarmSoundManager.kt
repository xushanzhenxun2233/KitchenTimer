package com.kitchentimer.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 响铃与震动管理器：使用手机系统默认闹钟铃声（回退系统默认铃声/提示音）
 */
class AlarmSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * 获取手机系统默认铃声 URI：优先闹钟默认铃声，其次来电默认铃声，最后通知默认铃声
     */
    private fun resolveDefaultAlarmUri(): Uri? {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    /**
     * 开始循环播放系统默认铃声并震动
     */
    @Synchronized
    fun startAlarm() {
        if (isPlaying) return
        val uri = resolveDefaultAlarmUri()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                if (uri != null) {
                    setDataSource(context, uri)
                }
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
            isPlaying = true
        } catch (_: Exception) {
            // 兜底：若闹钟通道准备失败，尝试使用通知默认铃声
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (fallbackUri != null) {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(context, fallbackUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                    isPlaying = true
                }
            } catch (_: Exception) {
                isPlaying = false
            }
        }

        startVibration()
    }

    /**
     * 停止响铃与震动
     */
    @Synchronized
    fun stopAlarm() {
        isPlaying = false
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
            // ignore release error
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
            // ignore vibration cancel error
        }
    }

    private fun startVibration() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val pattern = longArrayOf(0, 600, 400, 600, 800)
        try {
            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (_: Exception) {
            // ignore
        }
    }

    fun release() {
        stopAlarm()
    }
}
