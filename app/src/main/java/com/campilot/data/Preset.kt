package com.campilot.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey val id: String,
    val name: String,
    val cameraId: String,
    val zoomRatio: Float,
    val focusMode: String = "auto", // auto, locked
    val exposureLock: Boolean = false,
    val torch: Boolean = false,
    val resolution: String = "1080p",
    val fps: Int = 30,
    val switchDelayMs: Long = 300,
    val settleTimeMs: Long = 200
)
