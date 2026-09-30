package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Scene
import com.example.data.model.SourceType
import com.example.ui.components.DisclaimerCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentScenes by viewModel.recentScenes.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlackCharcoal)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LiveCanvas Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create, preview, record, and stream media scenes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            // Hero Create Scene Button
            Button(
                onClick = {
                    viewModel.createQuickScene(SourceType.CAMERA) { sceneId ->
                        onNavigateToStudio(sceneId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("create_scene_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create New Scene",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        item {
            // Quick Sources Section
            Text(
                text = "Quick Source Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sources = listOf(
                    Triple(SourceType.CAMERA, Icons.Default.Videocam, "Camera"),
                    Triple(SourceType.PHOTO, Icons.Default.Image, "Photo"),
                    Triple(SourceType.VIDEO, Icons.Default.Movie, "Video"),
                    Triple(SourceType.NETWORK, Icons.Default.CloudQueue, "Network"),
                    Triple(SourceType.SCREEN, Icons.Default.ScreenShare, "Screen Share"),
                    Triple(SourceType.PRIVACY_SLATE, Icons.Default.VisibilityOff, "Privacy Slate")
                )
                items(sources) { (type, icon, label) ->
                    QuickSourceCard(
                        icon = icon,
                        label = label,
                        onClick = {
                            viewModel.createQuickScene(type) { sceneId ->
                                onNavigateToStudio(sceneId)
                            }
                        }
                    )
                }
            }
        }

        item {
            // Mandatory Platform Boundary & Safety Card
            DisclaimerCard()
        }

        item {
            // Recent Scenes Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Scenes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${recentScenes.size} scenes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (recentScenes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No scenes created yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Create New Scene' or pick a Quick Source above to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(recentScenes) { scene ->
                RecentSceneItem(
                    scene = scene,
                    onClick = { onNavigateToStudio(scene.id) }
                )
            }
        }
    }
}

@Composable
fun QuickSourceCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(100.dp)
            .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("quick_source_$label"),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSlateSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TealAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun RecentSceneItem(
    scene: Scene,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("recent_scene_${scene.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSlateSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (scene.sourceType) {
                    SourceType.CAMERA -> Icons.Default.Videocam
                    SourceType.PHOTO -> Icons.Default.Image
                    SourceType.VIDEO -> Icons.Default.Movie
                    SourceType.NETWORK -> Icons.Default.CloudQueue
                    SourceType.SCREEN -> Icons.Default.ScreenShare
                    SourceType.PRIVACY_SLATE -> Icons.Default.VisibilityOff
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${scene.sourceType.displayName} • ${scene.aspectRatio.label} • ${scene.fps}fps",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Scene in Studio",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
