package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.PlaylistDetails
import com.arslandaim.playtube.domain.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GetPlaylistDetailsUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(playlistUrl: String): Result<PlaylistDetails> {
        return try {
            Result.success(repository.getPlaylistDetails(playlistUrl))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
