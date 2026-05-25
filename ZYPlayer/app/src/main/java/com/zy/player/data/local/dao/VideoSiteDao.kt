package com.zy.player.data.local.dao

import androidx.room.*
import com.zy.player.data.local.entity.VideoSiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoSiteDao {
    @Query("SELECT * FROM video_sites ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<VideoSiteEntity>>

    @Query("SELECT * FROM video_sites WHERE enabled = 1 ORDER BY sortOrder ASC, id ASC")
    suspend fun getEnabled(): List<VideoSiteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: VideoSiteEntity): Long

    @Update
    suspend fun update(site: VideoSiteEntity)

    @Delete
    suspend fun delete(site: VideoSiteEntity)

    @Query("DELETE FROM video_sites")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM video_sites")
    suspend fun count(): Int
}
