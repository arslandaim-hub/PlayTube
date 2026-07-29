/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_interests")
data class UserInterestEntity(
    @PrimaryKey val keyword: String,
    val weight: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)
