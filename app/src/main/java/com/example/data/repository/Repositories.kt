package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SceneRepository(private val sceneDao: SceneDao) {

    val allScenes: Flow<List<Scene>> = sceneDao.getAllScenes().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getSceneById(id: String): Scene? {
        return sceneDao.getSceneById(id)?.toDomain()
    }

    suspend fun saveScene(scene: Scene) {
        sceneDao.insertScene(scene.toEntity())
    }

    suspend fun deleteScene(id: String) {
        sceneDao.deleteSceneById(id)
    }

    suspend fun clearAllScenes() {
        sceneDao.clearAllScenes()
    }

    private fun SceneEntity.toDomain(): Scene = Scene(
        id = id,
        name = name,
        sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.CAMERA),
        sourceUri = sourceUri,
        aspectRatio = runCatching { AspectRatioPreset.valueOf(aspectRatio) }.getOrDefault(AspectRatioPreset.RATIO_16_9),
        outputWidth = outputWidth,
        outputHeight = outputHeight,
        fps = fps,
        bitrateKbps = bitrateKbps,
        transformState = TransformState(
            scale = scale,
            translationX = translationX,
            translationY = translationY,
            rotationDegrees = rotationDegrees,
            horizontalFlip = horizontalFlip,
            verticalFlip = verticalFlip,
            opacity = opacity,
            fitMode = runCatching { FitMode.valueOf(fitMode) }.getOrDefault(FitMode.FIT),
            backgroundColor = backgroundColor,
            watermarkText = watermarkText,
            showSafeGuides = showSafeGuides
        ),
        audioConfig = AudioConfig(
            micEnabled = micEnabled,
            sourceAudioEnabled = sourceAudioEnabled,
            micVolume = micVolume,
            sourceVolume = sourceVolume
        ),
        loopVideo = loopVideo,
        thumbnailUri = thumbnailUri,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )

    private fun Scene.toEntity(): SceneEntity = SceneEntity(
        id = id,
        name = name,
        sourceType = sourceType.name,
        sourceUri = sourceUri,
        aspectRatio = aspectRatio.name,
        outputWidth = outputWidth,
        outputHeight = outputHeight,
        fps = fps,
        bitrateKbps = bitrateKbps,
        scale = transformState.scale,
        translationX = transformState.translationX,
        translationY = transformState.translationY,
        rotationDegrees = transformState.rotationDegrees,
        horizontalFlip = transformState.horizontalFlip,
        verticalFlip = transformState.verticalFlip,
        opacity = transformState.opacity,
        fitMode = transformState.fitMode.name,
        backgroundColor = transformState.backgroundColor,
        watermarkText = transformState.watermarkText,
        showSafeGuides = transformState.showSafeGuides,
        micEnabled = audioConfig.micEnabled,
        sourceAudioEnabled = audioConfig.sourceAudioEnabled,
        micVolume = audioConfig.micVolume,
        sourceVolume = audioConfig.sourceVolume,
        loopVideo = loopVideo,
        thumbnailUri = thumbnailUri,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}

class MediaRepository(private val recentMediaDao: RecentMediaDao) {
    val allRecentMedia: Flow<List<RecentMedia>> = recentMediaDao.getAllRecentMedia().map { list ->
        list.map { RecentMedia(it.id, it.uri, it.mediaType, it.displayName, it.lastUsedAt) }
    }

    suspend fun saveRecentMedia(recent: RecentMedia) {
        recentMediaDao.insertRecentMedia(RecentMediaEntity(recent.id, recent.uri, recent.mediaType, recent.displayName, recent.lastUsedAt))
    }

    suspend fun deleteRecentMedia(id: String) {
        recentMediaDao.deleteRecentMediaById(id)
    }

    suspend fun clearAllRecentMedia() {
        recentMediaDao.clearAllRecentMedia()
    }
}
