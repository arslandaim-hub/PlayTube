/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.LocalPlaylistEntity
import com.arslandaim.playtube.data.local.LocalPlaylistVideoEntity
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class BackupRestoreTest {

    private val gson = Gson()

    @Test
    fun testPlayTubeBackupSerialization() {
        val backup = PlayTubeBackup(
            version = 2,
            timestamp = 123456789L,
            history = listOf(
                HistoryEntity("v1", "Test Video", "https://img", "Test Channel")
            ),
            localPlaylists = listOf(
                LocalPlaylistEntity(1, "My Playlist", "Description")
            ),
            localPlaylistVideos = listOf(
                LocalPlaylistVideoEntity(1, 1, "v1", "Test Video", "https://img", "Test Channel", 180L)
            ),
            preferences = PlayTubePreferences(
                isHistoryEnabled = true,
                isPlayerGesturesEnabled = false
            )
        )

        val json = gson.toJson(backup)
        assertNotNull(json)

        val deserialized = gson.fromJson(json, PlayTubeBackup::class.java)
        assertNotNull(deserialized)
        assertEquals(2, deserialized.version)
        assertEquals(1, deserialized.history?.size)
        assertEquals("v1", deserialized.history?.first()?.videoId)
        assertEquals(1, deserialized.localPlaylists?.size)
        assertEquals("My Playlist", deserialized.localPlaylists?.first()?.name)
        assertEquals(false, deserialized.preferences?.isPlayerGesturesEnabled)
    }

    @Test
    fun testLegacyBackupDeserializationWithoutLocalPlaylists() {
        // Simulates an older backup JSON missing localPlaylists or new preference fields
        val legacyJson = """
            {
                "version": 1,
                "timestamp": 1000000,
                "history": [{"videoId": "old1", "title": "Old Video", "thumbnailUrl": "url", "uploaderName": "Channel"}],
                "favorites": [],
                "playlistFavorites": [],
                "subscriptions": [],
                "searchHistory": [],
                "userInterests": [],
                "preferences": {
                    "isHistoryEnabled": true,
                    "isPipEnabled": false
                }
            }
        """.trimIndent()

        val parsed = gson.fromJson(legacyJson, PlayTubeBackup::class.java)
        assertNotNull(parsed)
        assertEquals("old1", parsed.history?.first()?.videoId)
        assertNotNull(parsed.localPlaylists) // Defaulted to emptyList or null
        assertNotNull(parsed.localPlaylistVideos)
        assertEquals(true, parsed.preferences?.isHistoryEnabled)
    }
}
