package com.muista.app.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.muista_logo),
                            contentDescription = "MUISTA!",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = reminder?.formattedTime() ?: "MUISTA!",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MuistaBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = reminder?.whatToRemember?.ifBlank { "Muistutus!" } ?: "Muistutus!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        if (!reminder?.spokenText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"${reminder?.spokenText}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MuistaBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))

                        BlinkingStopSignButton(
                            onClick = {
                                val stopIntent = Intent(this@AlarmActivity, ReminderService::class.java).apply {
                                    action = ReminderService.ACTION_STOP
                                }
                                startService(stopIntent)
                                finish()
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Paina STOP-merkkiä lopettaaksesi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MuistaBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlinkingStopSignButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "stop_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stop_alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stop_scale"
    )

    Box(
        modifier = Modifier
            .size(190.dp)
            .scale(scale)
            .alpha(alpha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * 0.94f
            val cx = size.width / 2f
            val cy = size.height / 2f

            fun octagonPath(r: Float): Path {
                val path = Path()
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians(22.5 + i * 45.0)
                    val x = cx + (r * cos(angleRad)).toFloat()
                    val y = cy + (r * sin(angleRad)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                return path
            }

            drawPath(path = octagonPath(radius), color = Color.White)
            drawPath(path = octagonPath(radius * 0.92f), color = MuistaRed)
            drawPath(
                path = octagonPath(radius * 0.92f),
                color = Color.White,
                style = Stroke(width = 6f)
            )
        }

        Text(
            text = "!",
            fontSize = 104.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
