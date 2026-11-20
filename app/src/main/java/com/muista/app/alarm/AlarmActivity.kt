package com.muista.app.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muista.app.R
import com.muista.app.data.ReminderRepository
import com.muista.app.ui.theme.MuistaBlue
import com.muista.app.ui.theme.MuistaRed
import com.muista.app.ui.theme.MuistaTheme
import com.muista.app.ui.theme.MuistaYellow

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reminderId = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID)
        val reminder = reminderId?.let { ReminderRepository(this).getReminderById(it) }

        setContent {
            MuistaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(MuistaYellow, Color(0xFFFFB300))
                                )
                            )
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.muista_logo),
                            contentDescription = "MUISTA!",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(28.dp))
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = reminder?.formattedTime() ?: "MUISTA!",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MuistaBlue
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = reminder?.whatToRemember?.ifBlank { "Muistutus!" } ?: "Muistutus!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        if (!reminder?.spokenText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"${reminder?.spokenText}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MuistaBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = {
                                val stopIntent = Intent(this@AlarmActivity, ReminderService::class.java).apply {
                                    action = ReminderService.ACTION_STOP
                                }
                                startService(stopIntent)
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MuistaRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Text(
                                text = "KUITTAA / LOPETA",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
