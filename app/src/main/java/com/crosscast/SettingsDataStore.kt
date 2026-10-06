package com.crosscast

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val RTMP_URL = stringPreferencesKey("rtmp_url")
        private val STREAM_KEY = stringPreferencesKey("stream_key")
        private val CAMERA_ID = stringPreferencesKey("camera_id")
        private val RESOLUTION_WIDTH = intPreferencesKey("resolution_width")
        private val RESOLUTION_HEIGHT = intPreferencesKey("resolution_height")
        private val BITRATE = intPreferencesKey("bitrate")

        const val DEFAULT_RTMP_URL = "rtmp://saopaulo.restream.io/live/re_3115918_event69ba8c5af63a497d8077e9713f66559b"
        const val DEFAULT_CAMERA_ID = "0"
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 720
        const val DEFAULT_BITRATE = 2500000
    }

    val rtmpUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[RTMP_URL] ?: DEFAULT_RTMP_URL
    }

    val streamKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[STREAM_KEY] ?: ""
    }

    val cameraId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CAMERA_ID] ?: DEFAULT_CAMERA_ID
    }

    val resolutionWidth: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[RESOLUTION_WIDTH] ?: DEFAULT_WIDTH
    }

    val resolutionHeight: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[RESOLUTION_HEIGHT] ?: DEFAULT_HEIGHT
    }

    val bitrate: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BITRATE] ?: DEFAULT_BITRATE
    }

    suspend fun setRtmpUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[RTMP_URL] = url
        }
    }

    suspend fun setStreamKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[STREAM_KEY] = key
        }
    }

    suspend fun setCameraId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[CAMERA_ID] = id
        }
    }

    suspend fun setResolution(width: Int, height: Int) {
        context.dataStore.edit { prefs ->
            prefs[RESOLUTION_WIDTH] = width
            prefs[RESOLUTION_HEIGHT] = height
        }
    }

    suspend fun setBitrate(bitrate: Int) {
        context.dataStore.edit { prefs ->
            prefs[BITRATE] = bitrate
        }
    }

    // Full settings object
    data class StreamSettings(
        val rtmpUrl: String,
        val streamKey: String,
        val cameraId: String,
        val width: Int,
        val height: Int,
        val bitrate: Int
    )

    val streamSettings: Flow<StreamSettings> = context.dataStore.data.map { prefs ->
        StreamSettings(
            rtmpUrl = prefs[RTMP_URL] ?: DEFAULT_RTMP_URL,
            streamKey = prefs[STREAM_KEY] ?: "",
            cameraId = prefs[CAMERA_ID] ?: DEFAULT_CAMERA_ID,
            width = prefs[RESOLUTION_WIDTH] ?: DEFAULT_WIDTH,
            height = prefs[RESOLUTION_HEIGHT] ?: DEFAULT_HEIGHT,
            bitrate = prefs[BITRATE] ?: DEFAULT_BITRATE
        )
    }
}
