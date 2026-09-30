package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Scene
import com.example.data.model.SourceType
import com.example.data.repository.SceneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(private val sceneRepository: SceneRepository) : ViewModel() {

    val recentScenes: StateFlow<List<Scene>> = sceneRepository.allScenes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createQuickScene(sourceType: SourceType, onCreated: (String) -> Unit) {
        val newId = UUID.randomUUID().toString()
        val newScene = Scene(
            id = newId,
            name = "${sourceType.displayName} Scene",
            sourceType = sourceType
        )
        viewModelScope.launch {
            sceneRepository.saveScene(newScene)
            onCreated(newId)
        }
    }
}

class ScenesViewModel(private val sceneRepository: SceneRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val filteredScenes: StateFlow<List<Scene>> = combine(
        sceneRepository.allScenes,
        _searchQuery
    ) { scenes, query ->
        if (query.isBlank()) scenes
        else scenes.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun duplicateScene(scene: Scene) {
        viewModelScope.launch {
            val duplicate = scene.copy(
                id = UUID.randomUUID().toString(),
                name = "${scene.name} (Copy)",
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis()
            )
            sceneRepository.saveScene(duplicate)
        }
    }

    fun deleteScene(id: String) {
        viewModelScope.launch {
            sceneRepository.deleteScene(id)
        }
    }

    fun createNewScene(name: String, sourceType: SourceType, onCreated: (String) -> Unit) {
        val newId = UUID.randomUUID().toString()
        val newScene = Scene(
            id = newId,
            name = if (name.isBlank()) "Untitled Scene" else name,
            sourceType = sourceType
        )
        viewModelScope.launch {
            sceneRepository.saveScene(newScene)
            onCreated(newId)
        }
    }
}

class ViewModelFactory(
    private val sceneRepository: SceneRepository,
    private val preferencesRepository: com.example.data.datastore.StudioPreferencesRepository,
    private val mediaRepository: com.example.data.repository.MediaRepository,
    private val streamingEngine: com.example.engine.streaming.StreamingEngine
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(sceneRepository) as T
            modelClass.isAssignableFrom(ScenesViewModel::class.java) ->
                ScenesViewModel(sceneRepository) as T
            modelClass.isAssignableFrom(StudioViewModel::class.java) ->
                StudioViewModel(sceneRepository, streamingEngine, mediaRepository) as T
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(mediaRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(preferencesRepository, sceneRepository, mediaRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
        }
    }
}
