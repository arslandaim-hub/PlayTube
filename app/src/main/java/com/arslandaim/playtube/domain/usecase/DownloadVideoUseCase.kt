/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.repository.DownloadRepository
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        videoId: String,
        url: String?,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        quality: String?,
        format: String?,
        audioUrl: String? = null,
        playlistId: String? = null,
        playlistTitle: String? = null,
        subtitleUrl: String? = null,
        subtitleLanguage: String? = null
    ) {
        repository.startDownload(
            videoId = videoId,
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            uploaderName = uploaderName,
            quality = quality,
            format = format,
            audioUrl = audioUrl,
            playlistId = playlistId,
            playlistTitle = playlistTitle,
            subtitleUrl = subtitleUrl,
            subtitleLanguage = subtitleLanguage
        )
    }
}
