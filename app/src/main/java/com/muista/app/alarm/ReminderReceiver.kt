package com.muista.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.muista.app.data.ReminderRepository

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID) ?: return
        val repo = ReminderRepository(context)
        val reminder = repo.getReminderById(reminderId) ?: return
        val masterEnabled = repo.isMasterEnabled()

        // Reschedule the next occurrence immediately
        if (masterEnabled && reminder.isEnabled) {
            AlarmScheduler.scheduleReminder(context, reminder, true)
        } else {
            return
        }

        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
