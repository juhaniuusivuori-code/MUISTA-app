package com.muista.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muista.app.model.FINNISH_WEEKDAYS
import com.muista.app.model.Reminder
import com.muista.app.model.RepeatMode
import com.muista.app.model.SoundMode
import com.muista.app.ui.theme.MuistaBlue
import com.muista.app.ui.theme.MuistaGreen
import com.muista.app.ui.theme.MuistaRed
import com.muista.app.ui.theme.MuistaYellow
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorDialog(
    initialReminder: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val context = LocalContext.current

    var isEnabled by remember { mutableStateOf(initialReminder?.isEnabled ?: true) }
    var repeatMode by remember { mutableStateOf(initialReminder?.repeatMode ?: RepeatMode.WEEKLY) }
    var whatToRemember by remember { mutableStateOf(initialReminder?.whatToRemember ?: "") }
    var spokenText by remember { mutableStateOf(initialReminder?.spokenText ?: "") }
    var soundMode by remember { mutableStateOf(initialReminder?.soundMode ?: SoundMode.BOTH) }
    var alarmToneUri by remember { mutableStateOf(initialReminder?.alarmToneUri) }
    var selectedDays by remember {
        mutableStateOf(
            initialReminder?.selectedDays ?: FINNISH_WEEKDAYS.map { it.first }.toSet()
        )
    }
    var monthlyDayTimes by remember {
        mutableStateOf(initialReminder?.monthlyDayTimes ?: emptyMap())
    }
    var hour by remember { mutableIntStateOf(initialReminder?.hour ?: 9) }
    var minute by remember { mutableIntStateOf(initialReminder?.minute ?: 0) }
    var volumePercent by remember { mutableIntStateOf(initialReminder?.volumePercent ?: 80) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMonthlyCalendar by remember { mutableStateOf(false) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            alarmToneUri = uri?.toString()
        }
    }

    var previewTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.runCatching {
                if (isPlaying) stop()
                release()
            }
            previewTts?.runCatching {
                stop()
                shutdown()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialReminder == null) "Uusi muistutus" else "Muokkaa",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = MuistaBlue,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Button(
                    onClick = { isEnabled = !isEnabled },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabled) MuistaGreen else Color.Gray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isEnabled) "PÄÄLLÄ" else "POIS",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // VIIKKO MUISTUTUS / KUUKAUSI MUISTUTUS painike
                Text(
                    text = "Toistuvuus",
                    fontWeight = FontWeight.Bold,
                    color = MuistaBlue
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWeekly = repeatMode == RepeatMode.WEEKLY
                    Button(
                        onClick = { repeatMode = RepeatMode.WEEKLY },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWeekly) MuistaBlue else Color(0xFFE0E0E0),
                            contentColor = if (isWeekly) Color.White else MuistaBlue
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "VIIKKO\nMUISTUTUS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            repeatMode = RepeatMode.MONTHLY
                            showMonthlyCalendar = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isWeekly) MuistaBlue else Color(0xFFE0E0E0),
                            contentColor = if (!isWeekly) Color.White else MuistaBlue
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "KUUKAUSI\nMUISTUTUS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Päivät: Ma, Ti, Ke, To, Pe, La, Su TAI kuukauden päivä
                if (repeatMode == RepeatMode.WEEKLY) {
                    Text(
                        text = "Päivät (rastita minä päivinä muistutus tulee)",
                        fontWeight = FontWeight.Bold,
                        color = MuistaBlue
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FINNISH_WEEKDAYS.forEach { (day, label) ->
                            val checked = day in selectedDays
                            FilterChip(
                                selected = checked,
                                onClick = {
                                    selectedDays = if (checked) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = if (checked) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MuistaYellow,
                                    selectedLabelColor = MuistaBlue,
                                    selectedLeadingIconColor = MuistaRed
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Kuukauden päivät",
                        fontWeight = FontWeight.Bold,
                        color = MuistaBlue
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMonthlyCalendar = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8DC),
                        border = BorderStroke(1.5.dp, MuistaBlue)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MuistaBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (monthlyDayTimes.isEmpty()) {
                                        "Ei päiviä valittu"
                                    } else {
                                        monthlyDayTimes.entries
                                            .sortedBy { it.key }
                                            .joinToString(", ") { (d, hm) ->
                                                String.format("%d. (%02d:%02d)", d, hm.first, hm.second)
                                            }
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = MuistaBlue
                                )
                            }
                            Text(
                                text = "Valitse kalenterista",
                                color = MuistaRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Kellonaika (näytetään viikkomuistutukselle; kuukausimuistutuksessa jokaisella päivällä on oma aika)
                if (repeatMode == RepeatMode.WEEKLY) {
                    Text(
                        text = "Kellonaika (24h)",
                        fontWeight = FontWeight.Bold,
                        color = MuistaBlue
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8DC),
                        border = BorderStroke(1.5.dp, MuistaBlue)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MuistaBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = String.format("%02d:%02d", hour, minute),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MuistaBlue
                                )
                            }
                            Text(
                                text = "Vaihda aika",
                                color = MuistaRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Kenttä: Mitä pitää muistaa
                OutlinedTextField(
                    value = whatToRemember,
                    onValueChange = { whatToRemember = it },
                    label = { Text("Mitä pitää muistaa?") },
                    placeholder = { Text("Esim. Ota lääkkeet") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                // Kenttä: Mitä puhelin sanoo muistutushetkenä
                OutlinedTextField(
                    value = spokenText,
                    onValueChange = { spokenText = it },
                    label = { Text("Mitä puhelin sanoo muistutushetkenä") },
                    placeholder = { Text("Esim. Hei! Muista ottaa lääkkeet nyt.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                // Hälytysääni / Puhe -valinta
                Text(
                    text = "Hälytysääni ja puhe",
                    fontWeight = FontWeight.Bold,
                    color = MuistaBlue
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SoundMode.entries.forEach { mode ->
                        FilterChip(
                            selected = soundMode == mode,
                            onClick = { soundMode = mode },
                            label = { Text(mode.labelFi, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MuistaBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (soundMode != SoundMode.SPEECH_ONLY) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Valitse hälytysääni")
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    alarmToneUri?.let { Uri.parse(it) }
                                )
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (alarmToneUri == null) "Hälytysääni: Oletus (Vaihda)" else "Hälytysääni: Mukautettu (Vaihda)"
                        )
                    }
                }

                // VOLUME liukusäädin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MuistaBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VOLUME: $volumePercent%",
                            fontWeight = FontWeight.ExtraBold,
                            color = MuistaBlue
                        )
                    }
                    TextButton(
                        onClick = {
                            testAudio(
                                context = context,
                                spokenText = spokenText.ifBlank { whatToRemember.ifBlank { "Muista!" } },
                                soundMode = soundMode,
                                alarmToneUri = alarmToneUri,
                                volumePercent = volumePercent,
                                onTtsCreated = {
                                    previewTts?.shutdown()
                                    previewTts = it
                                },
                                onPlayerCreated = {
                                    previewPlayer?.runCatching {
                                        if (isPlaying) stop()
                                        release()
                                    }
                                    previewPlayer = it
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MuistaRed)
                        Text("Testaa ääni", color = MuistaRed, fontWeight = FontWeight.Bold)
                    }
                }

                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { volumePercent = it.roundToInt().coerceIn(0, 100) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MuistaRed,
                        activeTrackColor = MuistaBlue,
                        inactiveTrackColor = Color(0xFFD0D7DE)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saved = Reminder(
                        id = initialReminder?.id ?: java.util.UUID.randomUUID().toString(),
                        isEnabled = isEnabled,
                        repeatMode = repeatMode,
                        whatToRemember = whatToRemember.trim(),
                        spokenText = spokenText.trim(),
                        soundMode = soundMode,
                        alarmToneUri = alarmToneUri,
                        selectedDays = selectedDays,
                        monthlyDayTimes = monthlyDayTimes,
                        hour = hour,
                        minute = minute,
                        volumePercent = volumePercent
                    )
                    onSave(saved)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuistaBlue,
                    contentColor = Color.White
                )
            ) {
                Text("TALLENNA", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Peruuta", color = MuistaRed)
            }
        }
    )

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Valitse kellonaika", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                Button(
                    onClick = {
                        hour = timeState.hour
                        minute = timeState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Peruuta")
                }
            }
        )
    }

    if (showMonthlyCalendar) {
        MonthlyCalendarDialog(
            selectedDayTimes = monthlyDayTimes,
            defaultHour = hour,
            defaultMinute = minute,
            onSetDayTime = { day, h, m ->
                monthlyDayTimes = monthlyDayTimes + (day to (h to m))
            },
            onRemoveDay = { day ->
                monthlyDayTimes = monthlyDayTimes - day
            },
            onClearAll = { monthlyDayTimes = emptyMap() },
            onDismiss = { showMonthlyCalendar = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyCalendarDialog(
    selectedDayTimes: Map<Int, Pair<Int, Int>>,
    defaultHour: Int,
    defaultMinute: Int,
    onSetDayTime: (Int, Int, Int) -> Unit,
    onRemoveDay: (Int) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var pickingTimeForDay by remember { mutableStateOf<Int?>(null) }
    val currentMonth = remember { YearMonth.now() }
    val monthNamesFi = remember {
        listOf(
            "Tammikuu", "Helmikuu", "Maaliskuu", "Huhtikuu",
            "Toukokuu", "Kesäkuu", "Heinäkuu", "Elokuu",
            "Syyskuu", "Lokakuu", "Marraskuu", "Joulukuu"
        )
    }
    val monthTitle = "${monthNamesFi[currentMonth.monthValue - 1]} ${currentMonth.year}"
    val firstDayOffset = currentMonth.atDay(1).dayOfWeek.value - 1 // 0 = Ma .. 6 = Su
    val daysInMonth = currentMonth.lengthOfMonth()
    val totalCells = firstDayOffset + daysInMonth
    val weeks = (totalCells + 6) / 7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = monthTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MuistaBlue
                )
                Text(
                    text = "Paina päivää asettaaksesi ajan (24h). Uusi painallus poistaa.",
                    fontSize = 12.sp,
                    color = MuistaBlue.copy(alpha = 0.75f)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Ma", "Ti", "Ke", "To", "Pe", "La", "Su").forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MuistaBlue
                        )
                    }
                }
                for (week in 0 until weeks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = week * 7 + col
                            val dayNumber = cellIndex - firstDayOffset + 1
                            if (dayNumber in 1..daysInMonth) {
                                val assignedTime = selectedDayTimes[dayNumber]
                                val isSelected = assignedTime != null
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MuistaYellow else Color(0xFFF3F4F6)
                                        )
                                        .clickable {
                                            if (isSelected) {
                                                onRemoveDay(dayNumber)
                                            } else {
                                                pickingTimeForDay = dayNumber
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNumber",
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MuistaRed else MuistaBlue
                                        )
                                        if (assignedTime != null) {
                                            Text(
                                                text = String.format("%02d:%02d", assignedTime.first, assignedTime.second),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MuistaBlue,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuistaBlue,
                    contentColor = Color.White
                )
            ) {
                Text("VALMIS", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClearAll) {
                Text("TYHJENNÄ", color = MuistaRed, fontWeight = FontWeight.Bold)
            }
        }
    )

    val dayToPick = pickingTimeForDay
    if (dayToPick != null) {
        val dayTimeState = rememberTimePickerState(
            initialHour = defaultHour,
            initialMinute = defaultMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickingTimeForDay = null },
            title = {
                Text(
                    text = "Kellonaika ($dayToPick. päivä, 24h)",
                    fontWeight = FontWeight.Bold,
                    color = MuistaBlue
                )
            },
            text = { TimePicker(state = dayTimeState) },
            confirmButton = {
                Button(
                    onClick = {
                        onSetDayTime(dayToPick, dayTimeState.hour, dayTimeState.minute)
                        pickingTimeForDay = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MuistaBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingTimeForDay = null }) {
                    Text("Peruuta", color = MuistaRed)
                }
            }
        )
    }
}

private fun testAudio(
    context: Context,
    spokenText: String,
    soundMode: SoundMode,
    alarmToneUri: String?,
    volumePercent: Int,
    onTtsCreated: (TextToSpeech) -> Unit,
    onPlayerCreated: (MediaPlayer) -> Unit
) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    val targetVol = ((maxVol * volumePercent) / 100f).roundToInt().coerceIn(0, maxVol)
    try {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
    } catch (_: SecurityException) {
    }

    val handler = android.os.Handler(android.os.Looper.getMainLooper())

    fun startPreviewSpeech() {
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale("fi", "FI"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault())
                }
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (volumePercent / 100f).coerceIn(0f, 1f))
                }
                tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, "muista_preview")
            }
        }
        onTtsCreated(tts)
    }

    if (soundMode == SoundMode.SPEECH_ONLY) {
        startPreviewSpeech()
        return
    }

    val uri = alarmToneUri?.let { Uri.parse(it) }
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    if (uri == null) {
        if (soundMode == SoundMode.BOTH) startPreviewSpeech()
        return
    }

    try {
        val vol = (volumePercent / 100f).coerceIn(0f, 1f)
        var speechTriggered = false
        lateinit var player: MediaPlayer
        val proceedToSpeech = Runnable {
            if (!speechTriggered) {
                speechTriggered = true
                player.runCatching {
                    if (isPlaying) stop()
                    release()
                }
                if (soundMode == SoundMode.BOTH) {
                    handler.postDelayed({ startPreviewSpeech() }, 500L)
                }
            }
        }
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            prepare()
            setVolume(vol, vol)
            setOnCompletionListener {
                handler.removeCallbacks(proceedToSpeech)
                proceedToSpeech.run()
            }
            start()
        }
        onPlayerCreated(player)
        if (soundMode == SoundMode.BOTH) {
            handler.postDelayed(proceedToSpeech, 3000L)
        }
    } catch (_: Exception) {
        if (soundMode == SoundMode.BOTH) startPreviewSpeech()
    }
}
