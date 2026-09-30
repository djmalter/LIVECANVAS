package com.example.ui.viewmodel

import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.MediaRepository
import com.example.data.repository.SceneRepository
import com.example.engine.streaming.FakeStreamingEngine
import com.example.engine.streaming.StreamStats
import com.example.engine.streaming.StreamingEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class StudioViewModel(
    private val sceneRepository: SceneRepository,
    private val streamingEngine: StreamingEngine,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _currentScene = MutableStateFlow<Scene?>(null)
    val currentScene: StateFlow<Scene?> = _currentScene.asStateFlow()

    // Camera settings
    private val _cameraLens = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val cameraLens: StateFlow<Int> = _cameraLens.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isFrontMirrored = MutableStateFlow(true)
    val isFrontMirrored: StateFlow<Boolean> = _isFrontMirrored.asStateFlow()

    // Screen projection state
    private val _isScreenShareActive = MutableStateFlow(false)
    val isScreenShareActive: StateFlow<Boolean> = _isScreenShareActive.asStateFlow()

    // Streaming state
    val streamStats: StateFlow<StreamStats> = streamingEngine.statsFlow
    private val _serverUrl = MutableStateFlow("rtmp://live.example.com/app")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _streamKey = MutableStateFlow("live_key_sample_abc123")
    val streamKey: StateFlow<String> = _streamKey.asStateFlow()

    private val _isStreamKeyVisible = MutableStateFlow(false)
    val isStreamKeyVisible: StateFlow<Boolean> = _isStreamKeyVisible.asStateFlow()

    private val _showGoLiveDialog = MutableStateFlow(false)
    val showGoLiveDialog: StateFlow<Boolean> = _showGoLiveDialog.asStateFlow()

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private var recordTimerJob: Job? = null

    // Audio VU Meter (0.0f .. 1.0f)
    private val _audioVuMeter = MutableStateFlow(0f)
    val audioVuMeter: StateFlow<Float> = _audioVuMeter.asStateFlow()

    // Network Source Confirmation
    private val _networkUrlInput = MutableStateFlow("")
    val networkUrlInput: StateFlow<String> = _networkUrlInput.asStateFlow()

    private val _networkUrlError = MutableStateFlow<String?>(null)
    val networkUrlError: StateFlow<String?> = _networkUrlError.asStateFlow()

    init {
        // VU meter loop while mic or streaming is active
        viewModelScope.launch {
            while (isActive) {
                delay(120)
                val scene = _currentScene.value
                val isMicOn = scene?.audioConfig?.micEnabled == true
                val isLive = streamStats.value.isLive
                val isRec = _isRecording.value

                if (isMicOn && (isLive || isRec)) {
                    val base = (scene.audioConfig.micVolume * 0.7f).coerceIn(0.1f, 1f)
                    val jitter = (Math.random().toFloat() * 0.35f * base)
                    _audioVuMeter.value = (jitter + base * 0.4f).coerceIn(0f, 1f)
                } else if (isMicOn) {
                    _audioVuMeter.value = (Math.random().toFloat() * 0.25f).coerceIn(0f, 1f)
                } else {
                    _audioVuMeter.value = 0f
                }
            }
        }
    }

    fun loadScene(sceneId: String) {
        viewModelScope.launch {
            val scene = sceneRepository.getSceneById(sceneId) ?: Scene(
                id = sceneId,
                name = "Live Studio Scene",
                sourceType = SourceType.CAMERA
            )
            _currentScene.value = scene
        }
    }

    fun updateSceneName(name: String) {
        _currentScene.value = _currentScene.value?.copy(name = name)
        saveCurrentScene()
    }

    fun setSourceType(sourceType: SourceType) {
        _currentScene.value = _currentScene.value?.copy(sourceType = sourceType)
        saveCurrentScene()
    }

    fun setSourceUri(uri: String) {
        _currentScene.value = _currentScene.value?.copy(sourceUri = uri)
        saveCurrentScene()
        viewModelScope.launch {
            mediaRepository.saveRecentMedia(
                RecentMedia(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    mediaType = _currentScene.value?.sourceType?.displayName ?: "Media",
                    displayName = "Imported ${_currentScene.value?.sourceType?.displayName ?: "Source"}"
                )
            )
        }
    }

    fun toggleCameraLens() {
        _cameraLens.value = if (_cameraLens.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun toggleFrontMirror() {
        _isFrontMirrored.value = !_isFrontMirrored.value
    }

    fun setScreenShareActive(active: Boolean) {
        _isScreenShareActive.value = active
    }

    // Transform Updates
    fun updateTransform(update: (TransformState) -> TransformState) {
        val current = _currentScene.value ?: return
        _currentScene.value = current.copy(transformState = update(current.transformState))
        saveCurrentScene()
    }

    fun resetTransform() {
        updateTransform { TransformState(backgroundColor = it.backgroundColor) }
    }

    fun centerTransform() {
        updateTransform { it.copy(translationX = 0f, translationY = 0f) }
    }

    fun applyPreset(presetName: String) {
        when (presetName) {
            "Fit" -> updateTransform { it.copy(fitMode = FitMode.FIT, scale = 1.0f, translationX = 0f, translationY = 0f) }
            "Fill" -> updateTransform { it.copy(fitMode = FitMode.FILL, scale = 1.25f, translationX = 0f, translationY = 0f) }
            "Centered" -> updateTransform { it.copy(translationX = 0f, translationY = 0f) }
            "Mirror H" -> updateTransform { it.copy(horizontalFlip = !it.horizontalFlip) }
            "Flip V" -> updateTransform { it.copy(verticalFlip = !it.verticalFlip) }
            "Rotate 90" -> updateTransform { it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f) }
            "Privacy Slate" -> {
                setSourceType(SourceType.PRIVACY_SLATE)
                updateTransform { it.copy(backgroundColor = 0xFF000000) }
            }
        }
    }

    fun setAspectRatio(ratio: AspectRatioPreset) {
        _currentScene.value = _currentScene.value?.copy(aspectRatio = ratio)
        saveCurrentScene()
    }

    fun setWatermarkText(text: String) {
        updateTransform { it.copy(watermarkText = text) }
    }

    fun toggleSafeGuides() {
        updateTransform { it.copy(showSafeGuides = !it.showSafeGuides) }
    }

    // Audio Updates
    fun toggleMicrophone() {
        val current = _currentScene.value ?: return
        val currentMic = current.audioConfig.micEnabled
        _currentScene.value = current.copy(
            audioConfig = current.audioConfig.copy(micEnabled = !currentMic)
        )
        saveCurrentScene()
    }

    fun toggleSourceAudio() {
        val current = _currentScene.value ?: return
        val currentSrc = current.audioConfig.sourceAudioEnabled
        _currentScene.value = current.copy(
            audioConfig = current.audioConfig.copy(sourceAudioEnabled = !currentSrc)
        )
        saveCurrentScene()
    }

    fun setMicVolume(vol: Float) {
        val current = _currentScene.value ?: return
        _currentScene.value = current.copy(
            audioConfig = current.audioConfig.copy(micVolume = vol)
        )
    }

    fun setSourceVolume(vol: Float) {
        val current = _currentScene.value ?: return
        _currentScene.value = current.copy(
            audioConfig = current.audioConfig.copy(sourceVolume = vol)
        )
    }

    // Streaming
    fun setServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun setStreamKey(key: String) {
        _streamKey.value = key
    }

    fun toggleStreamKeyVisibility() {
        _isStreamKeyVisible.value = !_isStreamKeyVisible.value
    }

    fun openGoLiveDialog() {
        _showGoLiveDialog.value = true
    }

    fun dismissGoLiveDialog() {
        _showGoLiveDialog.value = false
    }

    fun startStreaming() {
        _showGoLiveDialog.value = false
        val scene = _currentScene.value ?: return
        viewModelScope.launch {
            streamingEngine.startStream(
                serverUrl = _serverUrl.value,
                streamKey = _streamKey.value,
                width = scene.outputWidth,
                height = scene.outputHeight,
                fps = scene.fps,
                bitrateKbps = scene.bitrateKbps
            )
        }
    }

    fun stopStreaming() {
        viewModelScope.launch {
            streamingEngine.stopStream()
        }
    }

    // Recording
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _recordingDuration.value = 0L
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _recordingDuration.value++
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        recordTimerJob?.cancel()
        recordTimerJob = null
    }

    // Network Source Validation
    fun setNetworkUrlInput(url: String) {
        _networkUrlInput.value = url
        _networkUrlError.value = null
    }

    fun confirmNetworkUrl(onConfirmed: (String) -> Unit) {
        val url = _networkUrlInput.value.trim()
        if (url.isBlank()) {
            _networkUrlError.value = "URL cannot be empty."
            return
        }
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true) &&
            !url.startsWith("rtsp://", ignoreCase = true)) {
            _networkUrlError.value = "Supported schemes are https://, http://, or rtsp://"
            return
        }
        _networkUrlError.value = null
        setSourceType(SourceType.NETWORK)
        setSourceUri(url)
        onConfirmed(url)
    }

    private fun saveCurrentScene() {
        val scene = _currentScene.value ?: return
        viewModelScope.launch {
            sceneRepository.saveScene(scene.copy(lastUsedAt = System.currentTimeMillis()))
        }
    }
}
