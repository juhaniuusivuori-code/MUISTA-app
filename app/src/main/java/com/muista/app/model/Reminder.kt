package com.muista.app.model

import java.time.DayOfWeek
import java.util.UUID

enum class RepeatMode {
    WEEKLY,
    MONTHLY
}

enum class SoundMode(val labelFi: String) {
    BOTH("Puhe + Hälytysääni"),
    SPEECH_ONLY("Vain puhe (TTS)"),
    ALARM_ONLY("Vain hälytysääni")
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val isEnabled: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.WEEKLY,
    val whatToRemember: String = "",
    val spokenText: String = "",
    val soundMode: SoundMode = SoundMode.BOTH,
    val alarmToneUri: String? = null,
    val selectedDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),
    val monthlyDays: Set<Int> = setOf(1),
    val hour: Int = 9,
    val minute: Int = 0,
    val volumePercent: Int = 80
) {
    val alarmRequestCode: Int
        get() = id.hashCode()

    fun formattedTime(): String = String.format("%02d:%02d", hour, minute)

    fun effectiveSpokenText(): String =
        spokenText.trim().ifEmpty { whatToRemember.trim().ifEmpty { "Muista!" } }
}

val FINNISH_WEEKDAYS: List<Pair<DayOfWeek, String>> = listOf(
    DayOfWeek.MONDAY to "Ma",
    DayOfWeek.TUESDAY to "Ti",
    DayOfWeek.WEDNESDAY to "Ke",
    DayOfWeek.THURSDAY to "To",
    DayOfWeek.FRIDAY to "Pe",
    DayOfWeek.SATURDAY to "La",
    DayOfWeek.SUNDAY to "Su"
)
