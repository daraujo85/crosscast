package com.crosscast.camera

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class CameraCommand(val cameraId: String, val zoom: Float, val flash: Boolean? = null)

object CameraControlRegistry {
    private val _commands = MutableSharedFlow<CameraCommand>(extraBufferCapacity = 1)
    val commands = _commands.asSharedFlow()

    fun emitCommand(cameraId: String, zoom: Float, flash: Boolean? = null) {
        _commands.tryEmit(CameraCommand(cameraId, zoom, flash))
    }
}
