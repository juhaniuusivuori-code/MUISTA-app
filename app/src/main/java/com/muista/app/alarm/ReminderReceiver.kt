package com.muista.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.muista.app.data.ReminderRepository

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID) ?: return
        val remainingRepeats = intent.getIntExtra(AlarmScheduler.EXTRA_REMAINING_REPEATS, -1)
        val repo = ReminderRepository(context)
        val reminder = repo.getReminderById(reminderId) ?: return
        val masterEnabled = repo.isMasterEnabled()

        if (!masterEnabled || !reminder.isEnabled) return

        if (remainingRepeats < 0) {
            // Initial scheduled trigger: reschedule next weekly/monthly occurrence
            AlarmScheduler.scheduleReminder(context, reminder, true)
            if (reminder.repeatCount > 1) {
                AlarmScheduler.scheduleRepeatFollowUp(context, reminder, reminder.repeatCount - 1)
            }
        } else if (remainingRepeats > 1) {
            // Follow-up repeat: schedule the next repeat if more remain
            AlarmScheduler.scheduleRepeatFollowUp(context, reminder, remainingRepeats - 1)
        }

        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
