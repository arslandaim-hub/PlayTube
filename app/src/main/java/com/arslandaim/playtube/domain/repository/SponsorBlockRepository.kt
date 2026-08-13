/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.domain.model.SponsorSegment

interface SponsorBlockRepository {
    suspend fun getSponsorSegments(videoId: String): Result<List<SponsorSegment>>
}
