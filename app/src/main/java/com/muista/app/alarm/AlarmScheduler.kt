package com.muista.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.muista.app.MainActivity
import com.muista.app.data.ReminderRepository
import com.muista.app.model.Reminder
import com.muista.app.model.RepeatMode
import java.time.YearMonth
import java.time.ZonedDateTime

object AlarmScheduler {

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_REMAINING_REPEATS = "extra_remaining_repeats"

    fun computeNextTriggerMillis(reminder: Reminder, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        return when (reminder.repeatMode) {
            RepeatMode.WEEKLY -> {
                if (reminder.selectedDays.isEmpty()) return null
                for (offset in 0L..7L) {
                    val candidateDate = now.toLocalDate().plusDays(offset)
                    if (candidateDate.dayOfWeek in reminder.selectedDays) {
                        val candidate = candidateDate
                            .atTime(reminder.hour, reminder.minute, 0, 0)
                            .atZone(now.zone)
                        if (candidate.isAfter(now)) {
                            return candidate.toInstant().toEpochMilli()
                        }
                    }
                }
                null
            }

            RepeatMode.MONTHLY -> {
                if (reminder.monthlyDateTimes.isEmpty()) return null
                reminder.monthlyDateTimes.entries
                    .map { (date, hm) ->
                        date.atTime(hm.first, hm.second, 0, 0).atZone(now.zone)
                    }
                    .filter { it.isAfter(now) }
                    .minOfOrNull { it.toInstant().toEpochMilli() }
            }
        }
    }

    fun scheduleReminder(context: Context, reminder: Reminder, masterEnabled: Boolean) {
        cancelReminder(context, reminder)
        if (!masterEnabled || !reminder.isEnabled) return

        val triggerAtMillis = computeNextTriggerMillis(reminder) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, reminder)

        val showIntent = PendingIntent.getActivity(
            context,
            reminder.alarmRequestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, reminder))
        cancelRepeatFollowUp(context, reminder)
    }

    fun scheduleRepeatFollowUp(context: Context, reminder: Reminder, remainingRepeats: Int) {
        if (remainingRepeats <= 0) return
        val triggerAtMillis = System.currentTimeMillis() + reminder.repeatIntervalMinutes * 60_000L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildRepeatPendingIntent(context, reminder, remainingRepeats)
        val showIntent = PendingIntent.getActivity(
            context,
            reminder.alarmRequestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelRepeatFollowUp(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildRepeatPendingIntent(context, reminder, 0))
    }

    fun rescheduleAll(context: Context) {
        val repo = ReminderRepository(context)
        val masterEnabled = repo.isMasterEnabled()
        val reminders = repo.getReminders()
        reminders.forEach { scheduleReminder(context, it, masterEnabled) }
    }

    private fun buildPendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.muista.app.ACTION_TRIGGER_REMINDER"
            data = Uri.parse("muista://reminder/${reminder.id}")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMAINING_REPEATS, -1)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.alarmRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildRepeatPendingIntent(
        context: Context,
        reminder: Reminder,
        remainingRepeats: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.muista.app.ACTION_REPEAT_REMINDER"
            data = Uri.parse("muista://repeat/${reminder.id}")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMAINING_REPEATS, remainingRepeats)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.alarmRequestCode xor 0x5A5A,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
