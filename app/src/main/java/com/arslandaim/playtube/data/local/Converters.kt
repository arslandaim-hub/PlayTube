/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.TypeConverter
import com.arslandaim.playtube.domain.model.VideoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromVideoItemList(value: List<VideoItem>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toVideoItemList(value: String?): List<VideoItem>? {
        if (value == null) return null
        return try {
            val listType = object : TypeToken<List<VideoItem>>() {}.type
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            com.arslandaim.playtube.utils.PTLog.e("Converters", "Failed to deserialize VideoItem list", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromMissionStatus(status: MissionStatus): String = status.name

    @TypeConverter
    fun toMissionStatus(name: String): MissionStatus = MissionStatus.valueOf(name)

    @TypeConverter
    fun fromChunkType(type: ChunkType): String = type.name

    @TypeConverter
    fun toChunkType(name: String): ChunkType = ChunkType.valueOf(name)
}
