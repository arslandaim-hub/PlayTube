package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadEntity>> = repository.getAllDownloads()
}
