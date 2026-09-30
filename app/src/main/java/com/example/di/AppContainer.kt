package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.datastore.StudioPreferencesRepository
import com.example.data.db.AppDatabase
import com.example.data.repository.MediaRepository
import com.example.data.repository.SceneRepository
import com.example.engine.streaming.FakeStreamingEngine
import com.example.engine.streaming.StreamingEngine

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "livecanvas_studio.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    val sceneRepository: SceneRepository by lazy {
        SceneRepository(database.sceneDao())
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(database.recentMediaDao())
    }

    val preferencesRepository: StudioPreferencesRepository by lazy {
        StudioPreferencesRepository(context.applicationContext)
    }

    val streamingEngine: StreamingEngine by lazy {
        FakeStreamingEngine()
    }
}
