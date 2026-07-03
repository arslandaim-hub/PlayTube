package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsSubscribedUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(channelId: String): Flow<Boolean> = repository.isSubscribed(channelId)
}
