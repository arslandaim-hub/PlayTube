/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.SponsorSegment
import com.arslandaim.playtube.domain.repository.SponsorBlockRepository
import com.arslandaim.playtube.utils.PTLog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SponsorBlockRepositoryImpl @Inject constructor(
    private val client: HttpClient
) : SponsorBlockRepository {
    
    private val baseUrl = "https://sponsor.ajay.app/api/skipSegments"

    override suspend fun getSponsorSegments(videoId: String): Result<List<SponsorSegment>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get(baseUrl) {
                url {
                    parameters.append("videoID", videoId)
                    // We can specify categories if needed, by default it might return all or some.
                    // categories=["sponsor","selfpromo","interaction","intro","outro","preview","music_offtopic"]
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val segments = response.body<List<SponsorSegment>>()
                Result.success(segments)
            } else if (response.status == HttpStatusCode.NotFound) {
                // No segments found for this video
                Result.success(emptyList())
            } else {
                Result.failure(Exception("Failed to fetch segments: ${response.status}"))
            }
        } catch (e: Exception) {
            PTLog.e("SponsorBlock", "Error fetching segments for $videoId", e)
            Result.failure(e)
        }
    }
}
