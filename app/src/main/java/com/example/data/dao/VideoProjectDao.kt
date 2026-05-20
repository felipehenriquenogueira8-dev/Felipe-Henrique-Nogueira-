package com.example.data.dao

import androidx.room.*
import com.example.data.entity.VideoProject
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): VideoProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("DELETE FROM video_projects")
    suspend fun clearAllProjects()
}
