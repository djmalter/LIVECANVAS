package com.example.ui.screens.studio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.engine.streaming.ConnectionState
import com.example.service.NotificationHelper
import com.example.service.StudioForegroundService
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    sceneId: String,
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sceneId) {
        viewModel.loadScene(sceneId)
    }

    BackHandler {
        onNavigateBack()
    }

    val scene by viewModel.currentScene.collectAsStateWithLifecycle()
    val cameraLens by viewModel.cameraLens.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val isFrontMirrored by viewModel.isFrontMirrored.collectAsStateWithLifecycle()
    val isScreenShareActive by viewModel.isScreenShareActive.collectAsStateWithLifecycle()
    val streamStats by viewModel.streamStats.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()
    val audioVuMeter by viewModel.audioVuMeter.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val streamKey by viewModel.streamKey.collectAsStateWithLifecycle()
    val isStreamKeyVisible by viewModel.isStreamKeyVisible.collectAsStateWithLifecycle()
    val showGoLiveDialog by viewModel.showGoLiveDialog.collectAsStateWithLifecycle()
    val networkUrlInput by viewModel.networkUrlInput.collectAsStateWithLifecycle()
    val networkUrlError by viewModel.networkUrlError.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            viewModel.setSourceType(SourceType.CAMERA)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleMicrophone()
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Media Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSourceType(SourceType.PHOTO)
            viewModel.setSourceUri(it.toString())
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSourceType(SourceType.VIDEO)
            viewModel.setSourceUri(it.toString())
        }
    }

    // Screen Share Launcher
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            viewModel.setSourceType(SourceType.SCREEN)
            viewModel.setScreenShareActive(true)
            val serviceIntent = Intent(context, StudioForegroundService::class.java).apply {
                putExtra(StudioForegroundService.EXTRA_TITLE, "Screen Share Live")
                putExtra(StudioForegroundService.EXTRA_DESC, "Device screen projection active in LiveCanvas")
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            Toast.makeText(context, "Screen share started via system projection", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setScreenShareActive(false)
            Toast.makeText(context, "Screen share cancelled or denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NearBlackCharcoal,
        topBar = {
            StudioTopBar(
                sceneName = scene?.name ?: "Studio Scene",
                isLive = streamStats.isLive,
                isRecording = isRecording,
                recordingDuration = recordingDuration,
                sourceType = scene?.sourceType ?: SourceType.CAMERA,
                isScreenActive = isScreenShareActive,
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Central Studio Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(scene?.transformState?.backgroundColor ?: 0xFF0B0D10))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentTransform = scene?.transformState ?: TransformState()
                val aspectPreset = scene?.aspectRatio ?: AspectRatioPreset.RATIO_16_9

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(aspectPreset.ratio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, DarkSlateBorder, RoundedCornerShape(8.dp))
                        .background(Color(currentTransform.backgroundColor))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                viewModel.updateTransform {
                                    it.copy(
                                        scale = (it.scale * zoom).coerceIn(0.2f, 5.0f),
                                        translationX = it.translationX + pan.x,
                                        translationY = it.translationY + pan.y,
                                        rotationDegrees = (it.rotationDegrees + rotation) % 360f
                                    )
                                }
                            }
                        }
                        .testTag("preview_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    // Render Source
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = currentTransform.scale * if (currentTransform.horizontalFlip) -1f else 1f
                                scaleY = currentTransform.scale * if (currentTransform.verticalFlip) -1f else 1f
                                translationX = currentTransform.translationX
                                translationY = currentTransform.translationY
                                rotationZ = currentTransform.rotationDegrees
                                alpha = currentTransform.opacity
                            }
                    ) {
                        when (scene?.sourceType) {
                            SourceType.CAMERA -> {
                                if (hasCameraPermission) {
                                    CameraPreviewView(
                                        cameraLens = cameraLens,
                                        isTorchOn = isTorchOn,
                                        isMirrored = isFrontMirrored && cameraLens == CameraSelector.LENS_FACING_FRONT
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(DarkSlateSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.VideocamOff,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Camera permission required", color = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                                Text("Grant Permission")
                                            }
                                        }
                                    }
                                }
                            }
                            SourceType.PHOTO -> {
                                if (scene?.sourceUri != null) {
                                    AsyncImage(
                                        model = scene?.sourceUri,
                                        contentDescription = "Scene Photo Source",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    PlaceholderSourceView(
                                        icon = Icons.Default.AddPhotoAlternate,
                                        label = "Tap 'Source' below to pick a photo"
                                    )
                                }
                            }
                            SourceType.VIDEO -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(DarkSlateSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = TealAccent,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (scene?.sourceUri != null) "Video Loaded (Loop Active)" else "Select a video from Source tab",
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            SourceType.NETWORK -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(DarkSlateSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudQueue,
                                            contentDescription = null,
                                            tint = ElectricIndigo,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = scene?.sourceUri ?: "Network Media Stream",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            SourceType.SCREEN -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.ScreenShare,
                                            contentDescription = null,
                                            tint = if (isScreenShareActive) TealAccent else TextSecondary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isScreenShareActive) "Screen Projection Active" else "Screen Share Standby",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            SourceType.PRIVACY_SLATE, null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentTransform.watermarkText.ifBlank { "PRIVACY SLATE" },
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Watermark Overlay
                    if (currentTransform.watermarkText.isNotBlank()) {
                        Text(
                            text = currentTransform.watermarkText,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        )
                    }

                    // Safe-Area Guidelines Overlay
                    if (currentTransform.showSafeGuides) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            // Bottom Studio Drawer / Controls
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                color = DarkSlateSurface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Control Tabs
                    ScrollableTabRow(
                        selectedTabIndex = activeTab,
                        containerColor = DarkSlateSurfaceVariant,
                        contentColor = Color.White,
                        edgePadding = 8.dp
                    ) {
                        val tabs = listOf("Source", "Transform", "Audio", "Record", "Stream", "Settings")
                        tabs.forEachIndexed { index, label ->
                            Tab(
                                selected = activeTab == index,
                                onClick = { activeTab = index },
                                text = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (activeTab) {
                            0 -> SourceControls(
                                currentType = scene?.sourceType ?: SourceType.CAMERA,
                                cameraLens = cameraLens,
                                isTorchOn = isTorchOn,
                                isMirrored = isFrontMirrored,
                                onSelectType = { type ->
                                    if (type == SourceType.CAMERA && !hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else if (type == SourceType.SCREEN) {
                                        val mediaProjectionManager =
                                            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                    } else {
                                        viewModel.setSourceType(type)
                                    }
                                },
                                onPickPhoto = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPickVideo = {
                                    videoPickerLauncher.launch("video/*")
                                },
                                onToggleCameraLens = { viewModel.toggleCameraLens() },
                                onToggleTorch = { viewModel.toggleTorch() },
                                onToggleMirror = { viewModel.toggleFrontMirror() },
                                networkUrl = networkUrlInput,
                                onNetworkUrlChange = { viewModel.setNetworkUrlInput(it) },
                                onConfirmNetworkUrl = { viewModel.confirmNetworkUrl { /* confirmed */ } },
                                networkError = networkUrlError
                            )
                            1 -> TransformControls(
                                transform = scene?.transformState ?: TransformState(),
                                onUpdate = { update -> viewModel.updateTransform(update) },
                                onReset = { viewModel.resetTransform() },
                                onCenter = { viewModel.centerTransform() },
                                onApplyPreset = { preset -> viewModel.applyPreset(preset) }
                            )
                            2 -> AudioControls(
                                audioConfig = scene?.audioConfig ?: AudioConfig(),
                                vuMeter = audioVuMeter,
                                onToggleMic = {
                                    if (!hasMicPermission) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.toggleMicrophone()
                                    }
                                },
                                onToggleSourceAudio = { viewModel.toggleSourceAudio() },
                                onMicVolumeChange = { viewModel.setMicVolume(it) },
                                onSourceVolumeChange = { viewModel.setSourceVolume(it) }
                            )
                            3 -> RecordControls(
                                isRecording = isRecording,
                                durationSeconds = recordingDuration,
                                onToggleRecord = {
                                    if (!isRecording) {
                                        val serviceIntent = Intent(context, StudioForegroundService::class.java).apply {
                                            putExtra(StudioForegroundService.EXTRA_TITLE, "Recording Scene")
                                            putExtra(StudioForegroundService.EXTRA_DESC, "Scene recording in progress")
                                        }
                                        ContextCompat.startForegroundService(context, serviceIntent)
                                    }
                                    viewModel.toggleRecording()
                                }
                            )
                            4 -> StreamControls(
                                streamStats = streamStats,
                                serverUrl = serverUrl,
                                streamKey = streamKey,
                                isKeyVisible = isStreamKeyVisible,
                                onServerUrlChange = { viewModel.setServerUrl(it) },
                                onStreamKeyChange = { viewModel.setStreamKey(it) },
                                onToggleKeyVisibility = { viewModel.toggleStreamKeyVisibility() },
                                onGoLive = { viewModel.openGoLiveDialog() },
                                onStopStream = { viewModel.stopStreaming() }
                            )
                            5 -> SceneSettingsControls(
                                scene = scene ?: Scene("dummy", "Scene", SourceType.CAMERA),
                                onSetAspectRatio = { viewModel.setAspectRatio(it) },
                                onSetWatermark = { viewModel.setWatermarkText(it) },
                                onToggleSafeGuides = { viewModel.toggleSafeGuides() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Go Live Confirmation Dialog
    if (showGoLiveDialog) {
        val currentSc = scene ?: Scene("sample", "Scene", SourceType.CAMERA)
        AlertDialog(
            onDismissRequest = { viewModel.dismissGoLiveDialog() },
            title = {
                Text(
                    text = "Confirm Go Live Broadcast",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Review broadcast settings before publishing live:", color = TextSecondary, fontSize = 13.sp)
                    HorizontalDivider(color = DarkSlateBorder)
                    ParamRow("Server Host", serverUrl.substringBefore("/").take(28))
                    ParamRow("Scene Name", currentSc.name)
                    ParamRow("Resolution", "${currentSc.outputWidth}x${currentSc.outputHeight}")
                    ParamRow("Frame Rate", "${currentSc.fps} FPS")
                    ParamRow("Video Bitrate", "${currentSc.bitrateKbps} Kbps")
                    ParamRow("Microphone", if (currentSc.audioConfig.micEnabled) "Active" else "Muted")
                    HorizontalDivider(color = DarkSlateBorder)
                    Text(
                        "Broadcast will initiate an outbound RTMP stream to the destination you specified. A persistent notification will remain active.",
                        fontSize = 11.sp,
                        color = TealAccent
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val serviceIntent = Intent(context, StudioForegroundService::class.java).apply {
                            putExtra(StudioForegroundService.EXTRA_TITLE, "Live Streaming")
                            putExtra(StudioForegroundService.EXTRA_DESC, "Publishing scene to RTMP endpoint")
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                        viewModel.startStreaming()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger),
                    modifier = Modifier.testTag("confirm_go_live_button")
                ) {
                    Text("Go Live Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissGoLiveDialog() }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSlateSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StudioTopBar(
    sceneName: String,
    isLive: Boolean,
    isRecording: Boolean,
    recordingDuration: Long,
    sourceType: SourceType,
    isScreenActive: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSlateSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("studio_back_button")) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Text(
            text = sceneName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveStatusChip(isLive = isLive)
            RecordingStatusChip(isRecording = isRecording, durationSeconds = recordingDuration)
            if (!isLive && !isRecording) {
                SimpleStateChip(label = "PREVIEW", containerColor = DarkSlateSurfaceVariant)
            }
            if (sourceType == SourceType.CAMERA) {
                SimpleStateChip(label = "CAM", containerColor = DeepViolet)
            } else if (isScreenActive) {
                SimpleStateChip(label = "SCREEN", containerColor = TealAccent, contentColor = Color.Black)
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    cameraLens: Int,
    isTorchOn: Boolean,
    isMirrored: Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraLens)
                    .build()

                runCatching {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(isTorchOn)
                    }
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier
            .fillMaxSize()
            .scale(scaleX = if (isMirrored) -1f else 1f, scaleY = 1f)
    )
}

@Composable
fun PlaceholderSourceView(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun SourceControls(
    currentType: SourceType,
    cameraLens: Int,
    isTorchOn: Boolean,
    isMirrored: Boolean,
    onSelectType: (SourceType) -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleMirror: () -> Unit,
    networkUrl: String,
    onNetworkUrlChange: (String) -> Unit,
    onConfirmNetworkUrl: () -> Unit,
    networkError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Active Media Source", fontWeight = FontWeight.Bold, color = Color.White)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SourceType.CAMERA to "Camera",
                SourceType.PHOTO to "Photo",
                SourceType.VIDEO to "Video",
                SourceType.NETWORK to "Stream URL",
                SourceType.SCREEN to "Screen",
                SourceType.PRIVACY_SLATE to "Slate"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = currentType == type,
                    onClick = { onSelectType(type) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        when (currentType) {
            SourceType.CAMERA -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggleCameraLens,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlateSurfaceVariant)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (cameraLens == CameraSelector.LENS_FACING_FRONT) "Front" else "Back")
                    }
                    Button(
                        onClick = onToggleTorch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTorchOn) AmberWarning else DarkSlateSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.FlashlightOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Torch")
                    }
                    Button(
                        onClick = onToggleMirror,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMirrored) TealAccent else DarkSlateSurfaceVariant
                        )
                    ) {
                        Text(if (isMirrored) "Mirrored" else "Normal", color = if (isMirrored) Color.Black else Color.White)
                    }
                }
            }
            SourceType.PHOTO -> {
                Button(
                    onClick = onPickPhoto,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photo from Device")
                }
            }
            SourceType.VIDEO -> {
                Button(
                    onClick = onPickVideo,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Video File")
                }
            }
            SourceType.NETWORK -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = networkUrl,
                        onValueChange = onNetworkUrlChange,
                        label = { Text("Stream URL (HTTPS or RTSP)") },
                        placeholder = { Text("https://example.com/live/stream.m3u8") },
                        isError = networkError != null,
                        supportingText = { if (networkError != null) Text(networkError, color = CoralDanger) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = onConfirmNetworkUrl,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load & Validate URL")
                    }
                }
            }
            SourceType.SCREEN -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Screen Sharing uses Android's standard MediaProjection consent.",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Android will display its system dialog before capture begins. LiveCanvas Studio shows a persistent notification while active.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            SourceType.PRIVACY_SLATE -> {
                Text(
                    "Solid black privacy slate active. Ideal for standby, breaks, or branding.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun TransformControls(
    transform: TransformState,
    onUpdate: ((TransformState) -> TransformState) -> Unit,
    onReset: () -> Unit,
    onCenter: () -> Unit,
    onApplyPreset: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Canvas Transforms", fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onCenter) { Text("Center", fontSize = 12.sp) }
                TextButton(onClick = onReset) { Text("Reset", fontSize = 12.sp, color = CoralDanger) }
            }
        }

        // Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Fit", "Fill", "Mirror H", "Flip V", "Rotate 90").forEach { preset ->
                Button(
                    onClick = { onApplyPreset(preset) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSlateSurfaceVariant)
                ) {
                    Text(preset, fontSize = 11.sp)
                }
            }
        }

        // Scale Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scale: ${String.format("%.1fx", transform.scale)}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
            Slider(
                value = transform.scale,
                onValueChange = { scaleVal -> onUpdate { it.copy(scale = scaleVal) } },
                valueRange = 0.5f..3.0f,
                modifier = Modifier.weight(1f)
            )
        }

        // Opacity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Opacity: ${(transform.opacity * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
            Slider(
                value = transform.opacity,
                onValueChange = { op -> onUpdate { it.copy(opacity = op) } },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AudioControls(
    audioConfig: AudioConfig,
    vuMeter: Float,
    onToggleMic: () -> Unit,
    onToggleSourceAudio: () -> Unit,
    onMicVolumeChange: (Float) -> Unit,
    onSourceVolumeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Audio Routing & Levels", fontWeight = FontWeight.Bold, color = Color.White)

        AudioVuMeter(level = vuMeter)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Microphone Input", color = Color.White, fontSize = 13.sp)
            Switch(checked = audioConfig.micEnabled, onCheckedChange = { onToggleMic() })
        }

        if (audioConfig.micEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mic Vol", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                Slider(
                    value = audioConfig.micVolume,
                    onValueChange = onMicVolumeChange,
                    valueRange = 0f..2.0f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Source Audio", color = Color.White, fontSize = 13.sp)
            Switch(checked = audioConfig.sourceAudioEnabled, onCheckedChange = { onToggleSourceAudio() })
        }
    }
}

@Composable
fun RecordControls(
    isRecording: Boolean,
    durationSeconds: Long,
    onToggleRecord: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        Text(
            text = String.format("%02d:%02d", mins, secs),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) TealAccent else Color.White
        )
        Text(
            text = if (isRecording) "Recording MP4 into system MediaStore..." else "Ready to capture scene",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Button(
            onClick = onToggleRecord,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp)
                .testTag("studio_record_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) CoralDanger else ElectricIndigo
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}

@Composable
fun StreamControls(
    streamStats: com.example.engine.streaming.StreamStats,
    serverUrl: String,
    streamKey: String,
    isKeyVisible: Boolean,
    onServerUrlChange: (String) -> Unit,
    onStreamKeyChange: (String) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onGoLive: () -> Unit,
    onStopStream: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (streamStats.isLive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RTMP Transmission Live", color = CoralDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val mins = streamStats.durationSeconds / 60
                        val secs = streamStats.durationSeconds % 60
                        Text(String.format("%02d:%02d", mins, secs), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bitrate: ${streamStats.bitrateKbps} Kbps", fontSize = 12.sp, color = TextSecondary)
                        Text("FPS: ${streamStats.fps}", fontSize = 12.sp, color = TextSecondary)
                        Text("Drops: ${streamStats.droppedFrames}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
            Button(
                onClick = onStopStream,
                colors = ButtonDefaults.buttonColors(containerColor = CoralDanger),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Live Stream")
            }
        } else {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = { Text("RTMP Server URL") },
                placeholder = { Text("rtmp://live.twitch.tv/app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = streamKey,
                onValueChange = onStreamKeyChange,
                label = { Text("Stream Key") },
                visualTransformation = if (isKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleKeyVisibility) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Key Visibility"
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onGoLive,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("go_live_button")
            ) {
                Icon(Icons.Default.Podcasts, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Prepare & Go Live")
            }
        }
    }
}

@Composable
fun SceneSettingsControls(
    scene: Scene,
    onSetAspectRatio: (AspectRatioPreset) -> Unit,
    onSetWatermark: (String) -> Unit,
    onToggleSafeGuides: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Canvas & Preset Settings", fontWeight = FontWeight.Bold, color = Color.White)
        Text("Target Aspect Ratio", fontSize = 12.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AspectRatioPreset.RATIO_16_9 to "16:9",
                AspectRatioPreset.RATIO_9_16 to "9:16",
                AspectRatioPreset.RATIO_1_1 to "1:1",
                AspectRatioPreset.RATIO_4_5 to "4:5"
            ).forEach { (preset, label) ->
                FilterChip(
                    selected = scene.aspectRatio == preset,
                    onClick = { onSetAspectRatio(preset) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        OutlinedTextField(
            value = scene.transformState.watermarkText,
            onValueChange = onSetWatermark,
            label = { Text("Watermark / Overlay Text") },
            placeholder = { Text("e.g. LIVE BROADCAST") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Safe-Area Alignment Guides", color = Color.White, fontSize = 13.sp)
            Switch(
                checked = scene.transformState.showSafeGuides,
                onCheckedChange = { onToggleSafeGuides() }
            )
        }
    }
}
