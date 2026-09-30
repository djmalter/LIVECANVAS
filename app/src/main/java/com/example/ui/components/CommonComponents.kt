package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.DarkSlateBorder
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.DarkSlateSurfaceVariant
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.TealAccent

@Composable
fun LiveStatusChip(
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLive) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = CoralDanger.copy(alpha = alpha),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.testTag("live_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecordingStatusChip(
    isRecording: Boolean,
    durationSeconds: Long,
    modifier: Modifier = Modifier
) {
    if (!isRecording) return

    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Surface(
        color = TealAccent.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.testTag("recording_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CoralDanger.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            Text(
                text = String.format("REC %02d:%02d", mins, secs),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SimpleStateChip(
    label: String,
    containerColor: Color = DarkSlateSurfaceVariant,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AudioVuMeter(
    level: Float, // 0.0 .. 1.0
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Audio VU Level", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format("%d%%", (level * 100).toInt()), fontSize = 11.sp, color = TealAccent)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkSlateBorder)
        ) {
            val progressWidth = level.coerceIn(0f, 1f)
            val barColor = when {
                progressWidth > 0.85f -> CoralDanger
                progressWidth > 0.65f -> Color(0xFFF59E0B)
                else -> TealAccent
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progressWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun DisclaimerCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security & Compliance Information",
                tint = TealAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "What this app can and cannot do",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LiveCanvas Studio creates and streams scenes from within its own recording and streaming workflow. It does not replace the Android system camera or inject media into third-party video-call applications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
