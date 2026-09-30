package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.StudioPreferencesRepository
import com.example.data.datastore.StudioSettings
import com.example.data.model.RecentMedia
import com.example.data.repository.MediaRepository
import com.example.data.repository.SceneRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val recentMedia: StateFlow<List<RecentMedia>> = mediaRepository.allRecentMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRecentMedia(id: String) {
        viewModelScope.launch {
            mediaRepository.deleteRecentMedia(id)
        }
    }

    fun clearRecentMedia() {
        viewModelScope.launch {
            mediaRepository.clearAllRecentMedia()
        }
    }
}

class SettingsViewModel(
    private val preferencesRepository: StudioPreferencesRepository,
    private val sceneRepository: SceneRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val settings: StateFlow<StudioSettings> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioSettings())

    fun updateDefaultResolution(res: String) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultResolution(res)
        }
    }

    fun updateDefaultFps(fps: Int) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultFps(fps)
        }
    }

    fun updateDefaultBitrate(bitrate: Int) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultBitrate(bitrate)
        }
    }

    fun updateKeepScreenAwake(keepAwake: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateKeepScreenAwake(keepAwake)
        }
    }

    fun updateAllowLocalNetworkSources(allow: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAllowLocalNetworkSources(allow)
        }
    }

    fun deleteAllAppData() {
        viewModelScope.launch {
            sceneRepository.clearAllScenes()
            mediaRepository.clearAllRecentMedia()
            preferencesRepository.clearSettings()
        }
    }
}
