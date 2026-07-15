/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncSubscriptionMetadataUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke() {
        val subscriptions = libraryRepository.getSubscriptions().first()
        
        subscriptions.filter { it.subscriberCount == null || it.subscriberCount == 0L }.forEach { sub ->
            try {
                val details = videoRepository.getChannelDetails(sub.channelId)
                if (details.subscriberCount != null && details.subscriberCount > 0) {
                    libraryRepository.subscribe(
                        SubscriptionEntity(
                            channelId = sub.channelId,
                            name = details.name,
                            thumbnailUrl = details.avatarUrl ?: sub.thumbnailUrl,
                            subscriberCount = details.subscriberCount
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip failed syncs to avoid blocking others
                e.printStackTrace()
            }
        }
    }
}
