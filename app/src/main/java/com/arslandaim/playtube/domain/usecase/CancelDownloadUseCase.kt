package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.repository.DownloadRepository
import javax.inject.Inject

class CancelDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(videoId: String) {
        repository.cancelDownload(videoId)
    }
}
