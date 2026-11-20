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
    private val handler = Handler(Looper.getMainLooper())

    private val speechRunnable = object : Runnable {
        override fun run() {
            val reminder = currentReminder ?: return
            if (ttsReady && (reminder.soundMode == SoundMode.SPEECH_ONLY || reminder.soundMode == SoundMode.BOTH)) {
                speakReminder(reminder)
            }
            handler.postDelayed(this, SPEECH_REPEAT_INTERVAL_MS)
        }
    }

    private val autoStopRunnable = Runnable {
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
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
        currentReminder = reminder
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(reminder))

        applyAlarmVolume(reminder.volumePercent)

        if (reminder.soundMode == SoundMode.ALARM_ONLY || reminder.soundMode == SoundMode.BOTH) {
            startAlarmSound(reminder)
        }
        if (reminder.soundMode == SoundMode.SPEECH_ONLY || reminder.soundMode == SoundMode.BOTH) {
            tts = TextToSpeech(this, this)
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
        if (status == TextToSpeech.SUCCESS) {
            val fiLocale = Locale("fi", "FI")
            val result = tts?.setLanguage(fiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            ttsReady = true
            handler.removeCallbacks(speechRunnable)
            handler.post(speechRunnable)
        }
    }

    private fun speakReminder(reminder: Reminder) {
        val text = reminder.effectiveSpokenText()
        val volumeFloat = (reminder.volumePercent / 100f).coerceIn(0f, 1f)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeFloat)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "muista_${reminder.id}")
    }

    private fun startAlarmSound(reminder: Reminder) {
        val uri: Uri = reminder.alarmToneUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        try {
            val volume = (reminder.volumePercent / 100f).coerceIn(0f, 1f)
            val playerVolume = if (reminder.soundMode == SoundMode.BOTH) (volume * 0.45f) else volume
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
                setVolume(playerVolume, playerVolume)
                start()
            }
        } catch (_: Exception) {
            // Ignore playback errors if device has no default alarm sound
        }
    }

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
            // Some DND modes block changing stream volume
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

    private fun stopPlayback() {
        handler.removeCallbacks(speechRunnable)
        handler.removeCallbacks(autoStopRunnable)
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
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
        private const val SPEECH_REPEAT_INTERVAL_MS = 5500L
        private const val AUTO_STOP_MS = 60_000L
    }
}
