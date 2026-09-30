package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DisclaimerCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlackCharcoal)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Studio Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Preferences, media defaults, and security",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item {
            DisclaimerCard()
        }

        item {
            SettingsSectionHeader(title = "Default Output Settings")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Resolution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Default Resolution", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Canvas target for recording & stream", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("720p", "1080p").forEach { res ->
                                FilterChip(
                                    selected = settings.defaultResolution == res,
                                    onClick = { viewModel.updateDefaultResolution(res) },
                                    label = { Text(res, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkSlateBorder)

                    // Frame Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Default Frame Rate", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Target capture FPS", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24, 30, 60).forEach { fps ->
                                FilterChip(
                                    selected = settings.defaultFps == fps,
                                    onClick = { viewModel.updateDefaultFps(fps) },
                                    label = { Text("${fps}fps", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkSlateBorder)

                    // Video Bitrate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Default Video Bitrate", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Encoding bitrate in Kbps", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2500, 4500, 8000).forEach { bitrate ->
                                FilterChip(
                                    selected = settings.defaultBitrateKbps == bitrate,
                                    onClick = { viewModel.updateDefaultBitrate(bitrate) },
                                    label = { Text("${bitrate / 1000}M", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(title = "Playback & Safety Controls")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keep Screen Awake While Live", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Prevents sleep during live streams or recording", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.keepScreenAwake,
                            onCheckedChange = { viewModel.updateKeepScreenAwake(it) }
                        )
                    }

                    HorizontalDivider(color = DarkSlateBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow Local Network Sources", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Enables LAN IP addresses (192.168.x.x, 10.x.x.x) for streams", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.allowLocalNetworkSources,
                            onCheckedChange = { viewModel.updateAllowLocalNetworkSources(it) }
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(title = "Privacy & Data")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "LiveCanvas Studio is built with Privacy-By-Design. No user accounts, zero analytics by default, and no background tracking.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralDanger.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralDanger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralDanger)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All App Data & Scenes", color = CoralDanger)
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(title = "App Information")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LiveCanvas Studio v1.0.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Native Android media composition and streaming tool.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Licensed under Apache 2.0. Built with Jetpack Compose & Material 3.", color = TealAccent, fontSize = 11.sp)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Data?", color = Color.White) },
            text = {
                Text(
                    "This will delete all saved scenes, recent media items, stream keys, and preferences. This action cannot be reversed.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllAppData()
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "All data wiped successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger)
                ) {
                    Text("Confirm Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSlateSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
