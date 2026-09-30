package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "studio_settings")

data class StudioSettings(
    val defaultResolution: String = "1080p",
    val defaultFps: Int = 30,
    val defaultBitrateKbps: Int = 4500,
    val keepScreenAwake: Boolean = true,
    val allowLocalNetworkSources: Boolean = false,
    val themeMode: String = "dark"
)

class StudioPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_DEFAULT_RES = stringPreferencesKey("default_resolution")
        val KEY_DEFAULT_FPS = intPreferencesKey("default_fps")
        val KEY_DEFAULT_BITRATE = intPreferencesKey("default_bitrate")
        val KEY_KEEP_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val KEY_ALLOW_LOCAL_NET = booleanPreferencesKey("allow_local_network")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<StudioSettings> = context.dataStore.data.map { preferences ->
        StudioSettings(
            defaultResolution = preferences[PreferencesKeys.KEY_DEFAULT_RES] ?: "1080p",
            defaultFps = preferences[PreferencesKeys.KEY_DEFAULT_FPS] ?: 30,
            defaultBitrateKbps = preferences[PreferencesKeys.KEY_DEFAULT_BITRATE] ?: 4500,
            keepScreenAwake = preferences[PreferencesKeys.KEY_KEEP_AWAKE] ?: true,
            allowLocalNetworkSources = preferences[PreferencesKeys.KEY_ALLOW_LOCAL_NET] ?: false,
            themeMode = preferences[PreferencesKeys.KEY_THEME_MODE] ?: "dark"
        )
    }

    suspend fun updateDefaultResolution(res: String) {
        context.dataStore.edit { it[PreferencesKeys.KEY_DEFAULT_RES] = res }
    }

    suspend fun updateDefaultFps(fps: Int) {
        context.dataStore.edit { it[PreferencesKeys.KEY_DEFAULT_FPS] = fps }
    }

    suspend fun updateDefaultBitrate(bitrate: Int) {
        context.dataStore.edit { it[PreferencesKeys.KEY_DEFAULT_BITRATE] = bitrate }
    }

    suspend fun updateKeepScreenAwake(keepAwake: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_KEEP_AWAKE] = keepAwake }
    }

    suspend fun updateAllowLocalNetworkSources(allow: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_ALLOW_LOCAL_NET] = allow }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { it.clear() }
    }
}
