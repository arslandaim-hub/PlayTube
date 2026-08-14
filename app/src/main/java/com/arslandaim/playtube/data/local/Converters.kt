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
        val listType = object : TypeToken<List<VideoItem>>() {}.type
        return gson.fromJson(value, listType)
    }
}
