package com.muista.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.muista.app.alarm.AlarmScheduler
import com.muista.app.data.ReminderRepository
import com.muista.app.ui.MainScreen
import com.muista.app.ui.theme.MuistaTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val repo = ReminderRepository(this)
        AlarmScheduler.rescheduleAll(this)

        setContent {
            MuistaTheme {
                var masterEnabled by remember { mutableStateOf(repo.isMasterEnabled()) }
                var reminders by remember { mutableStateOf(repo.getReminders()) }

                MainScreen(
                    masterEnabled = masterEnabled,
                    reminders = reminders,
                    onToggleMaster = {
                        val next = !masterEnabled
                        masterEnabled = next
                        repo.setMasterEnabled(next)
                        AlarmScheduler.rescheduleAll(this)
                    },
                    onSaveReminder = { saved ->
                        val updated = if (reminders.any { it.id == saved.id }) {
                            reminders.map { if (it.id == saved.id) saved else it }
                        } else {
                            reminders + saved
                        }
                        reminders = updated
                        repo.saveReminders(updated)
                        AlarmScheduler.scheduleReminder(this, saved, masterEnabled)
                    },
                    onToggleReminder = { target ->
                        val toggled = target.copy(isEnabled = !target.isEnabled)
                        val updated = reminders.map { if (it.id == target.id) toggled else it }
                        reminders = updated
                        repo.saveReminders(updated)
                        AlarmScheduler.scheduleReminder(this, toggled, masterEnabled)
                    },
                    onDeleteReminder = { target ->
                        AlarmScheduler.cancelReminder(this, target)
                        val updated = reminders.filterNot { it.id == target.id }
                        reminders = updated
                        repo.saveReminders(updated)
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName")
                )
                runCatching { startActivity(intent) }
            }
        }
    }
}
