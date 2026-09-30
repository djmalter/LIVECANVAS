package com.example.data.model

enum class SourceType(val displayName: String) {
    CAMERA("Camera"),
    PHOTO("Local Photo"),
    VIDEO("Local Video"),
    NETWORK("Network Stream"),
    SCREEN("Screen Share"),
    PRIVACY_SLATE("Privacy Slate")
}

enum class AspectRatioPreset(val label: String, val ratio: Float) {
    RATIO_16_9("16:9 Landscape", 16f / 9f),
    RATIO_9_16("9:16 Portrait", 9f / 16f),
    RATIO_1_1("1:1 Square", 1f),
    RATIO_4_5("4:5 Social", 4f / 5f),
    CUSTOM("Custom", 16f / 9f)
}

enum class FitMode {
    FIT, FILL, STRETCH
}

data class TransformState(
    val scale: Float = 1.0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val horizontalFlip: Boolean = false,
    val verticalFlip: Boolean = false,
    val opacity: Float = 1.0f,
    val fitMode: FitMode = FitMode.FIT,
    val backgroundColor: Long = 0xFF0B0D10,
    val watermarkText: String = "",
    val showSafeGuides: Boolean = true
)

data class AudioConfig(
    val micEnabled: Boolean = false,
    val sourceAudioEnabled: Boolean = true,
    val micVolume: Float = 1.0f,
    val sourceVolume: Float = 1.0f
)

data class Scene(
    val id: String,
    val name: String,
    val sourceType: SourceType,
    val sourceUri: String? = null,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_16_9,
    val outputWidth: Int = 1920,
    val outputHeight: Int = 1080,
    val fps: Int = 30,
    val bitrateKbps: Int = 4500,
    val transformState: TransformState = TransformState(),
    val audioConfig: AudioConfig = AudioConfig(),
    val loopVideo: Boolean = true,
    val thumbnailUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)

data class StreamDestination(
    val id: String,
    val displayName: String,
    val serverUrl: String,
    val streamKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)

data class RecentMedia(
    val id: String,
    val uri: String,
    val mediaType: String,
    val displayName: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)
