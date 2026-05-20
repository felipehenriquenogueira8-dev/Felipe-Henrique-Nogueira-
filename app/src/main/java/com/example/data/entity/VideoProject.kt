package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val characterName: String? = null,
    val characterDescription: String? = null,
    val duration: Int = 10,
    val resolution: String = "1080p",
    val isReferenceEdit: Boolean = false,
    val referencePrompt: String? = null,
    val imageUrl: String? = null, // Path to local generated image or base64 representation
    val status: String = "COMPLETED", // "GENERATING", "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis()
)
