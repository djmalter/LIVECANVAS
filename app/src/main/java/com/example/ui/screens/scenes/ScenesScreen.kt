package com.example.ui.screens.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Scene
import com.example.data.model.SourceType
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScenesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesScreen(
    viewModel: ScenesViewModel,
    onNavigateToStudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scenes by viewModel.filteredScenes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var sceneToDelete by remember { mutableStateOf<Scene?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NearBlackCharcoal,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ElectricIndigo,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("add_scene_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Scene")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header & View Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scenes Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = { viewModel.toggleViewMode() },
                    modifier = Modifier.testTag("view_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List View",
                        tint = TealAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_scenes_input"),
                placeholder = { Text("Search scenes by name...", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = DarkSlateBorder,
                    focusedContainerColor = DarkSlateSurface,
                    unfocusedContainerColor = DarkSlateSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (scenes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No scenes created yet" else "No matching scenes",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(scenes, key = { it.id }) { scene ->
                        SceneGridCard(
                            scene = scene,
                            onClick = { onNavigateToStudio(scene.id) },
                            onDuplicate = { viewModel.duplicateScene(scene) },
                            onDelete = { sceneToDelete = scene }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(scenes, key = { it.id }) { scene ->
                        SceneListCard(
                            scene = scene,
                            onClick = { onNavigateToStudio(scene.id) },
                            onDuplicate = { viewModel.duplicateScene(scene) },
                            onDelete = { sceneToDelete = scene }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (sceneToDelete != null) {
        val scene = sceneToDelete!!
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            title = { Text("Delete Scene", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to delete \"${scene.name}\"? This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScene(scene.id)
                        sceneToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSlateSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Create Scene Dialog
    if (showCreateDialog) {
        var newSceneName by remember { mutableStateOf("") }
        var selectedSource by remember { mutableStateOf(SourceType.CAMERA) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Scene", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSceneName,
                        onValueChange = { newSceneName = it },
                        label = { Text("Scene Name") },
                        placeholder = { Text("e.g. Broadcast Intro") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = DarkSlateBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Initial Media Source:", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val choices = listOf(SourceType.CAMERA, SourceType.PHOTO, SourceType.PRIVACY_SLATE)
                        choices.forEach { type ->
                            FilterChip(
                                selected = selectedSource == type,
                                onClick = { selectedSource = type },
                                label = { Text(type.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        viewModel.createNewScene(newSceneName, selectedSource) { id ->
                            onNavigateToStudio(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Create & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSlateSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SceneGridCard(
    scene: Scene,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("scene_card_${scene.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSlateSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (scene.sourceType) {
                        SourceType.CAMERA -> Icons.Default.Videocam
                        SourceType.PHOTO -> Icons.Default.Image
                        SourceType.VIDEO -> Icons.Default.Movie
                        SourceType.NETWORK -> Icons.Default.CloudQueue
                        SourceType.SCREEN -> Icons.Default.ScreenShare
                        SourceType.PRIVACY_SLATE -> Icons.Default.VisibilityOff
                    },
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = scene.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${scene.sourceType.displayName} • ${scene.aspectRatio.label}",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Duplicate scene",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete scene",
                        tint = CoralDanger,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SceneListCard(
    scene: Scene,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSlateBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("scene_list_card_${scene.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSlateSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (scene.sourceType) {
                        SourceType.CAMERA -> Icons.Default.Videocam
                        SourceType.PHOTO -> Icons.Default.Image
                        SourceType.VIDEO -> Icons.Default.Movie
                        SourceType.NETWORK -> Icons.Default.CloudQueue
                        SourceType.SCREEN -> Icons.Default.ScreenShare
                        SourceType.PRIVACY_SLATE -> Icons.Default.VisibilityOff
                    },
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scene.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${scene.sourceType.displayName} • ${scene.aspectRatio.label} • ${scene.outputWidth}x${scene.outputHeight}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onDuplicate) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Duplicate scene",
                    tint = TextSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete scene",
                    tint = CoralDanger
                )
            }
        }
    }
}
