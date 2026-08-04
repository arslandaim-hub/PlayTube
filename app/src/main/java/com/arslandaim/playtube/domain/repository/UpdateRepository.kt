/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface UpdateRepository {
    val updateInfo: StateFlow<UpdateInfo>
    suspend fun checkForUpdates()
}

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val updateUrl: String = ""
)
