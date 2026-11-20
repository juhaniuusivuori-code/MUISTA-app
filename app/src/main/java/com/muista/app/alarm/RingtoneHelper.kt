package com.muista.app.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object RingtoneHelper {

    fun resolveAlarmUri(context: Context, customUriString: String?): Uri? {
        if (!customUriString.isNullOrBlank()) {
            return Uri.parse(customUriString)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun getToneTitle(context: Context, customUriString: String?): String {
        if (customUriString.isNullOrBlank()) return "Oletus"
        return try {
            RingtoneManager.getRingtone(context, Uri.parse(customUriString))
                ?.getTitle(context)
                ?.takeIf { it.isNotBlank() }
                ?: "Valittu ääni"
        } catch (_: Exception) {
            "Valittu ääni"
        }
    }
}
