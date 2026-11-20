package com.muista.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.muista.app.R
import com.muista.app.data.ReminderRepository
import com.muista.app.model.Reminder
import com.muista.app.model.SoundMode
import java.util.Locale
import kotlin.math.roundToInt

class ReminderService : Service(), TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentReminder: Reminder? = null
    private var previousAlarmVolume: Int? = null
    private var isStopped = false
    private val handler = Handler(Looper.getMainLooper())

    private val autoStopRunnable = Runnable {
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            currentReminder?.let { AlarmScheduler.cancelRepeatFollowUp(this, it) }
            stopSelf()
            return START_NOT_STICKY
        }

        val reminderId = intent?.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID)
        val reminder = reminderId?.let { ReminderRepository(this).getReminderById(it) }
        if (reminder == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        stopPlayback()
        isStopped = false
        currentReminder = reminder
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(reminder))

        applyAlarmVolume(reminder.volumePercent)

        when (reminder.soundMode) {
            SoundMode.ALARM_ONLY -> startLoopingAlarmSound(reminder)
            SoundMode.SPEECH_ONLY -> tts = TextToSpeech(this, this)
            SoundMode.BOTH -> {
                // Initialize TTS first; once ready, run the sequential cycle: alarm -> pause -> speech
                tts = TextToSpeech(this, this)
            }
        }

        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, AUTO_STOP_MS)

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        startActivity(alarmIntent)

        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (isStopped) return
        val reminder = currentReminder ?: return

        if (status == TextToSpeech.SUCCESS) {
            val fiLocale = Locale("fi", "FI")
            val result = tts?.setLanguage(fiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (isStopped) return
                    handler.postDelayed({
                        if (!isStopped) startCycle(reminder)
                    }, CYCLE_PAUSE_MS)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (isStopped) return
                    handler.postDelayed({
                        if (!isStopped) startCycle(reminder)
                    }, CYCLE_PAUSE_MS)
                }
            })
            ttsReady = true
            startCycle(reminder)
        } else if (reminder.soundMode == SoundMode.BOTH) {
            // Fallback to looping alarm if TTS failed to initialize
            startLoopingAlarmSound(reminder)
        }
    }

    private fun startCycle(reminder: Reminder) {
        if (isStopped) return
        when (reminder.soundMode) {
            SoundMode.SPEECH_ONLY -> speakReminder(reminder)
            SoundMode.BOTH -> playAlarmBurstThenSpeak(reminder)
            SoundMode.ALARM_ONLY -> Unit
        }
    }

    private fun playAlarmBurstThenSpeak(reminder: Reminder) {
        val uri = resolveAlarmUri(reminder)
        if (uri == null) {
            speakReminder(reminder)
            return
        }

        releaseMediaPlayer()
        var triggeredSpeech = false
        val proceedToSpeech = Runnable {
            if (!triggeredSpeech && !isStopped) {
                triggeredSpeech = true
                releaseMediaPlayer()
                handler.postDelayed({
                    if (!isStopped) speakReminder(reminder)
                }, ALARM_TO_SPEECH_PAUSE_MS)
            }
        }

        try {
            val volume = (reminder.volumePercent / 100f).coerceIn(0f, 1f)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ReminderService, uri)
                isLooping = false
                prepare()
                setVolume(volume, volume)
                setOnCompletionListener {
                    handler.removeCallbacks(proceedToSpeech)
                    proceedToSpeech.run()
                }
                start()
            }
            // Cap long alarm ringtones at ALARM_BURST_MAX_MS so speech follows promptly
            handler.postDelayed(proceedToSpeech, ALARM_BURST_MAX_MS)
        } catch (_: Exception) {
            proceedToSpeech.run()
        }
    }

    private fun speakReminder(reminder: Reminder) {
        if (isStopped || !ttsReady) return
        val text = reminder.effectiveSpokenText()
        val volumeFloat = (reminder.volumePercent / 100f).coerceIn(0f, 1f)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeFloat)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "muista_${reminder.id}")
    }

    private fun startLoopingAlarmSound(reminder: Reminder) {
        val uri = resolveAlarmUri(reminder) ?: return
        try {
            val volume = (reminder.volumePercent / 100f).coerceIn(0f, 1f)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ReminderService, uri)
                isLooping = true
                prepare()
                setVolume(volume, volume)
                start()
            }
        } catch (_: Exception) {
        }
    }

    private fun resolveAlarmUri(reminder: Reminder): Uri? =
        reminder.alarmToneUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun applyAlarmVolume(volumePercent: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (previousAlarmVolume == null) {
            previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        val target = ((maxVol * volumePercent) / 100f).roundToInt().coerceIn(0, maxVol)
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
        } catch (_: SecurityException) {
        }
    }

    private fun restoreAlarmVolume() {
        val prev = previousAlarmVolume ?: return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
        } catch (_: SecurityException) {
        }
        previousAlarmVolume = null
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun stopPlayback() {
        isStopped = true
        handler.removeCallbacksAndMessages(null)
        releaseMediaPlayer()
        tts?.runCatching {
            stop()
            shutdown()
        }
        tts = null
        ttsReady = false
        restoreAlarmVolume()
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MUISTA! Hälytykset",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Muistutushälytykset ja puhe"
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(reminder: Reminder): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminder.alarmRequestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ReminderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            9999,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = reminder.whatToRemember.ifBlank { "MUISTA!" }
        val text = reminder.effectiveSpokenText()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MUISTA! • ${reminder.formattedTime()}")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$text"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "KUITTAA", stopPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.muista.app.ACTION_STOP_ALARM"
        private const val CHANNEL_ID = "muista_alarms_channel"
        private const val NOTIFICATION_ID = 4201
        private const val ALARM_BURST_MAX_MS = 3_000L
        private const val ALARM_TO_SPEECH_PAUSE_MS = 500L
        private const val CYCLE_PAUSE_MS = 1_500L
        private const val AUTO_STOP_MS = 45_000L
    }
}
