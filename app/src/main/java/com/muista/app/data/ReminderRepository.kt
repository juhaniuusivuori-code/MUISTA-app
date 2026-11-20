package com.muista.app.data

import android.content.Context
import com.muista.app.model.Reminder
import com.muista.app.model.RepeatMode
import com.muista.app.model.SoundMode
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

class ReminderRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isMasterEnabled(): Boolean = prefs.getBoolean(KEY_MASTER_ENABLED, true)

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
    }

    fun getReminders(): List<Reminder> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(fromJson(array.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveReminders(reminders: List<Reminder>) {
        val array = JSONArray()
        reminders.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    fun getReminderById(id: String): Reminder? = getReminders().firstOrNull { it.id == id }

    private fun toJson(reminder: Reminder): JSONObject {
        val daysArray = JSONArray()
        reminder.selectedDays.forEach { daysArray.put(it.value) }
        val monthlyTimesObj = JSONObject()
        reminder.monthlyDayTimes.toSortedMap().forEach { (day, hm) ->
            monthlyTimesObj.put(day.toString(), String.format("%02d:%02d", hm.first, hm.second))
        }
        return JSONObject().apply {
            put("id", reminder.id)
            put("isEnabled", reminder.isEnabled)
            put("repeatMode", reminder.repeatMode.name)
            put("whatToRemember", reminder.whatToRemember)
            put("spokenText", reminder.spokenText)
            put("soundMode", reminder.soundMode.name)
            put("alarmToneUri", reminder.alarmToneUri ?: JSONObject.NULL)
            put("selectedDays", daysArray)
            put("monthlyDayTimes", monthlyTimesObj)
            put("hour", reminder.hour)
            put("minute", reminder.minute)
            put("volumePercent", reminder.volumePercent)
        }
    }

    private fun fromJson(obj: JSONObject): Reminder {
        val daysJson = obj.optJSONArray("selectedDays") ?: JSONArray()
        val days = buildSet {
            for (i in 0 until daysJson.length()) {
                val value = daysJson.optInt(i, -1)
                if (value in 1..7) add(DayOfWeek.of(value))
            }
        }
        val defaultHour = obj.optInt("hour", 9).coerceIn(0, 23)
        val defaultMinute = obj.optInt("minute", 0).coerceIn(0, 59)

        val monthlyTimesObj = obj.optJSONObject("monthlyDayTimes")
        val monthlyDayTimes: Map<Int, Pair<Int, Int>> = if (monthlyTimesObj != null) {
            buildMap {
                val keys = monthlyTimesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val day = key.toIntOrNull() ?: continue
                    if (day !in 1..31) continue
                    val parts = monthlyTimesObj.optString(key, "").split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: defaultHour
                    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: defaultMinute
                    put(day, h to m)
                }
            }
        } else {
            val monthlyDaysJson = obj.optJSONArray("monthlyDays")
            if (monthlyDaysJson != null) {
                buildMap {
                    for (i in 0 until monthlyDaysJson.length()) {
                        val value = monthlyDaysJson.optInt(i, -1)
                        if (value in 1..31) put(value, defaultHour to defaultMinute)
                    }
                }
            } else {
                emptyMap()
            }
        }
        val repeatMode = runCatching {
            RepeatMode.valueOf(obj.optString("repeatMode", RepeatMode.WEEKLY.name))
        }.getOrDefault(RepeatMode.WEEKLY)

        val soundMode = runCatching {
            SoundMode.valueOf(obj.optString("soundMode", SoundMode.BOTH.name))
        }.getOrDefault(SoundMode.BOTH)

        val tone = if (obj.isNull("alarmToneUri")) null else obj.optString("alarmToneUri", "").ifEmpty { null }

        return Reminder(
            id = obj.getString("id"),
            isEnabled = obj.optBoolean("isEnabled", true),
            repeatMode = repeatMode,
            whatToRemember = obj.optString("whatToRemember", ""),
            spokenText = obj.optString("spokenText", ""),
            soundMode = soundMode,
            alarmToneUri = tone,
            selectedDays = days,
            monthlyDayTimes = monthlyDayTimes,
            hour = defaultHour,
            minute = defaultMinute,
            volumePercent = obj.optInt("volumePercent", 80).coerceIn(0, 100)
        )
    }

    companion object {
        private const val PREFS_NAME = "muista_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_REMINDERS = "reminders_json"
    }
}
