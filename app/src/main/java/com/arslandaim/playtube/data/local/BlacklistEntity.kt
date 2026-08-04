/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklist")
data class BlacklistEntity(
    @PrimaryKey
    val id: String, // Video or Channel ID
    val type: BlacklistType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BlacklistType {
    VIDEO,
    CHANNEL
}
