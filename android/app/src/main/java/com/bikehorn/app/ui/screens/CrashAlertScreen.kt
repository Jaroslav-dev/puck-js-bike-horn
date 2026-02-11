package com.bikehorn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bikehorn.app.crash.CrashAlertState

@Composable
fun CrashAlertScreen(
    state: CrashAlertState,
    emergencyContact: String,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = if (state.alarmPlaying) "ALARM ACTIVE" else "CRASH DETECTED",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Impact: ${state.magnitude}g",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
            )

            Spacer(Modifier.height(32.dp))

            if (!state.alarmPlaying) {
                // Countdown display
                Text(
                    text = "${state.secondsRemaining}",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.secondsRemaining <= 3) Color.Red else Color.White,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Sending emergency alert in ${state.secondsRemaining}s",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )

                if (emergencyContact.isNotBlank()) {
                    Text(
                        text = "Contact: $emergencyContact",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            } else {
                Text(
                    text = "Emergency alert sent",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier.size(width = 200.dp, height = 64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(
                    text = if (state.alarmPlaying) "STOP ALARM" else "I'M OK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
