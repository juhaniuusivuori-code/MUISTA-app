package com.muista.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muista.app.R
import com.muista.app.model.FINNISH_WEEKDAYS
import com.muista.app.model.Reminder
import com.muista.app.model.RepeatMode
import com.muista.app.ui.theme.MuistaBackground
import com.muista.app.ui.theme.MuistaBlue
import com.muista.app.ui.theme.MuistaGreen
import com.muista.app.ui.theme.MuistaRed
import com.muista.app.ui.theme.MuistaYellow

@Composable
fun MainScreen(
    masterEnabled: Boolean,
    reminders: List<Reminder>,
    onToggleMaster: () -> Unit,
    onSaveReminder: (Reminder) -> Unit,
    onToggleReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit
) {
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val sortedReminders = remember(reminders) {
        reminders.sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    Scaffold(
        containerColor = MuistaBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingReminder = null
                    showEditor = true
                },
                containerColor = MuistaRed,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Lisää muistutus", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header bar with logo, MUISTA! title, and PÄÄLLÄ / POIS master button
            Surface(
                color = MuistaYellow,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.muista_logo),
                            contentDescription = "MUISTA!",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MUISTA!",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MuistaBlue
                            )
                            Text(
                                text = if (masterEnabled) "Muistutukset käytössä" else "Kaikki muistutukset pois päältä",
                                fontSize = 12.sp,
                                color = MuistaBlue.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = onToggleMaster,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (masterEnabled) MuistaGreen else MuistaRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (masterEnabled) "PÄÄLLÄ" else "POIS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (sortedReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MuistaBlue.copy(alpha = 0.45f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ei vielä muistutuksia",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MuistaBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Paina \"+ Lisää muistutus\" luodaksesi ensimmäisen muistutuksen. Voit lisätä useita muistutuksia vaikka samalle päivälle.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MuistaBlue.copy(alpha = 0.75f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedReminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            masterEnabled = masterEnabled,
                            onClick = {
                                editingReminder = reminder
                                showEditor = true
                            },
                            onToggle = { onToggleReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder) }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        ReminderEditorDialog(
            initialReminder = editingReminder,
            onDismiss = { showEditor = false },
            onSave = { saved ->
                onSaveReminder(saved)
                showEditor = false
            }
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    masterEnabled: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val active = masterEnabled && reminder.isEnabled
    val cardAlpha = if (active) 1f else 0.6f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (active) MuistaBlue.copy(alpha = 0.25f) else Color.LightGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val headerTime = if (reminder.repeatMode == RepeatMode.WEEKLY) {
                        reminder.formattedTime()
                    } else {
                        reminder.monthlyDayTimes.values.minByOrNull { it.first * 60 + it.second }
                            ?.let { String.format("%02d:%02d", it.first, it.second) }
                            ?: "--:--"
                    }
                    Text(
                        text = headerTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MuistaBlue.copy(alpha = cardAlpha)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MuistaYellow.copy(alpha = if (active) 1f else 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (reminder.repeatMode == RepeatMode.WEEKLY) {
                                "VIIKKO MUISTUTUS"
                            } else {
                                "KUUKAUSI MUISTUTUS"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MuistaBlue
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (reminder.isEnabled) MuistaGreen else Color.Gray,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (reminder.isEnabled) "PÄÄLLÄ" else "POIS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Poista",
                            tint = MuistaRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reminder.whatToRemember.ifBlank { "(Ei otsikkoa)" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MuistaBlue.copy(alpha = cardAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (reminder.spokenText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Puhe: \"${reminder.spokenText}\"",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scheduleLabel = if (reminder.repeatMode == RepeatMode.WEEKLY) {
                    val dayLabels = FINNISH_WEEKDAYS
                        .filter { it.first in reminder.selectedDays }
                        .joinToString(", ") { it.second }
                    dayLabels.ifEmpty { "Ei päiviä valittu" }
                } else {
                    reminder.formattedMonthlySchedule()
                }

                Text(
                    text = scheduleLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MuistaRed.copy(alpha = cardAlpha)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = MuistaBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${reminder.volumePercent}% • ${reminder.soundMode.labelFi}",
                        fontSize = 12.sp,
                        color = MuistaBlue.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
