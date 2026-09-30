package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceType: String,
    val sourceUri: String?,
    val aspectRatio: String,
    val outputWidth: Int,
    val outputHeight: Int,
    val fps: Int,
    val bitrateKbps: Int,
    // Transform State
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val rotationDegrees: Float,
    val horizontalFlip: Boolean,
    val verticalFlip: Boolean,
    val opacity: Float,
    val fitMode: String,
    val backgroundColor: Long,
    val watermarkText: String,
    val showSafeGuides: Boolean,
    // Audio Config
    val micEnabled: Boolean,
    val sourceAudioEnabled: Boolean,
    val micVolume: Float,
    val sourceVolume: Float,
    val loopVideo: Boolean,
    val thumbnailUri: String?,
    val createdAt: Long,
    val lastUsedAt: Long
)

@Entity(tableName = "stream_destinations")
data class StreamDestinationEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val serverUrl: String,
    val streamKey: String,
    val createdAt: Long,
    val lastUsedAt: Long
)

@Entity(tableName = "recent_media")
data class RecentMediaEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val mediaType: String,
    val displayName: String,
    val lastUsedAt: Long
)

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes ORDER BY lastUsedAt DESC")
    fun getAllScenes(): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun getSceneById(id: String): SceneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity)

    @Query("DELETE FROM scenes WHERE id = :id")
    suspend fun deleteSceneById(id: String)

    @Query("DELETE FROM scenes")
    suspend fun clearAllScenes()
}

@Dao
interface StreamDestinationDao {
    @Query("SELECT * FROM stream_destinations ORDER BY lastUsedAt DESC")
    fun getAllDestinations(): Flow<List<StreamDestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: StreamDestinationEntity)

    @Query("DELETE FROM stream_destinations WHERE id = :id")
    suspend fun deleteDestinationById(id: String)
}

@Dao
interface RecentMediaDao {
    @Query("SELECT * FROM recent_media ORDER BY lastUsedAt DESC")
    fun getAllRecentMedia(): Flow<List<RecentMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentMedia(media: RecentMediaEntity)

    @Query("DELETE FROM recent_media WHERE id = :id")
    suspend fun deleteRecentMediaById(id: String)

    @Query("DELETE FROM recent_media")
    suspend fun clearAllRecentMedia()
}

@Database(
    entities = [SceneEntity::class, StreamDestinationEntity::class, RecentMediaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sceneDao(): SceneDao
    abstract fun streamDestinationDao(): StreamDestinationDao
    abstract fun recentMediaDao(): RecentMediaDao
}
