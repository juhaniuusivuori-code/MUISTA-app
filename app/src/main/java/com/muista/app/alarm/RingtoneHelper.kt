package com.muista.app.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object RingtoneHelper {

    private const val PREFERRED_TONE_NAME = "Universe Bell"

    fun resolveAlarmUri(context: Context, customUriString: String?): Uri? {
        if (!customUriString.isNullOrBlank()) {
            return Uri.parse(customUriString)
        }
        return findPreferredToneUri(context)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun findPreferredToneUri(context: Context): Uri? {
        val types = intArrayOf(
            RingtoneManager.TYPE_ALARM,
            RingtoneManager.TYPE_NOTIFICATION,
            RingtoneManager.TYPE_RINGTONE
        )
        for (type in types) {
            try {
                val manager = RingtoneManager(context).apply { setType(type) }
                val cursor = manager.cursor ?: continue
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: ""
                        if (title.contains(PREFERRED_TONE_NAME, ignoreCase = true)) {
                            return manager.getRingtoneUri(c.position)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }
}
