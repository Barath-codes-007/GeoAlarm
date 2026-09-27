package com.geoalarm.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.geoalarm.app.domain.model.AlarmSoundType
import com.geoalarm.app.domain.model.GeoAlarm

/**
 * Plays the arrival alarm. Section 7 / 11 requirements handled here:
 *  - default ringtone, a user-chosen audio file, vibration-only, or silent
 *  - never crash because a chosen file was deleted/became inaccessible/can't be decoded:
 *    always fall back to the default ringtone, and fall back again to vibration if even
 *    that fails.
 *
 * A process-wide singleton is acceptable here because only one alarm can be actively
 * ringing at a time (the ringing screen is `singleInstance`); a second arrival while one
 * is already ringing queues behind the user dismissing the first.
 */
object AlarmAudioController {
    private const val TAG = "AlarmAudioController"
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)

    fun start(context: Context, alarm: GeoAlarm) {
        stop() // never overlap a previous ringing sound
        val appContext = context.applicationContext

        val shouldVibrate = alarm.soundType != AlarmSoundType.SILENT
        if (shouldVibrate) startVibration(appContext)

        when (alarm.soundType) {
            AlarmSoundType.VIBRATION_ONLY, AlarmSoundType.SILENT -> Unit
            AlarmSoundType.DEFAULT_RINGTONE -> playUri(appContext, RingtoneManager.getActualDefaultRingtoneUri(
                appContext, RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            AlarmSoundType.NOTIFICATION_SOUND -> playUri(appContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            AlarmSoundType.CUSTOM_AUDIO -> {
                val uri = alarm.soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                if (uri != null) {
                    playUri(appContext, uri, fallbackToDefaultOnError = true)
                } else {
                    Log.w(TAG, "Custom audio URI missing; falling back to default ringtone")
                    playUri(appContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                }
            }
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
        stopVibration()
    }

    private fun playUri(context: Context, uri: Uri?, fallbackToDefaultOnError: Boolean = false) {
        if (uri == null) return
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, uri)
            player.isLooping = true
            player.setOnErrorListener { mp, what, extra ->
                Log.w(TAG, "MediaPlayer error ($what, $extra) for $uri")
                runCatching { mp.release() }
                if (fallbackToDefaultOnError) {
                    playUri(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                }
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            // Deleted file, permission revoked, unsupported codec, etc: never crash the alarm.
            Log.w(TAG, "Could not play $uri (${e.javaClass.simpleName}); falling back", e)
            if (fallbackToDefaultOnError) {
                playUri(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            }
            // If even the default ringtone fails, vibration (already started above) is the
            // remaining, always-available signal.
        }
    }

    private fun startVibration(context: Context) {
        val v = getVibrator(context) ?: return
        vibrator = v
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(vibrationPattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun getVibrator(context: Context): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
