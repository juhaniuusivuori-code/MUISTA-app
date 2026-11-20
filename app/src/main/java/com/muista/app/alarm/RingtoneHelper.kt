package com.muista.app.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.muista.app.data.ReminderRepository

object RingtoneHelper {

    fun resolveAlarmUri(context: Context, customUriString: String?): Uri? {
        val chosen = customUriString?.takeIf { it.isNotBlank() }
            ?: ReminderRepository(context).getDefaultAlarmToneUri()
        if (!chosen.isNullOrBlank()) {
            return Uri.parse(chosen)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun getToneTitle(context: Context, customUriString: String?): String {
        val effectiveUri = customUriString?.takeIf { it.isNotBlank() }
            ?: ReminderRepository(context).getDefaultAlarmToneUri()
        if (effectiveUri.isNullOrBlank()) return "Oletus"
        return try {
            RingtoneManager.getRingtone(context, Uri.parse(effectiveUri))
                ?.getTitle(context)
                ?.takeIf { it.isNotBlank() }
                ?: "Valittu ääni"
        } catch (_: Exception) {
            "Valittu ääni"
        }
    }
}
