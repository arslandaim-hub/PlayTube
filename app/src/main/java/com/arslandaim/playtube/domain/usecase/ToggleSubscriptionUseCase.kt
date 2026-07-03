package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleSubscriptionUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(subscription: SubscriptionEntity) {
        val isSubscribed = repository.isSubscribed(subscription.channelId).first()
        if (isSubscribed) {
            repository.unsubscribe(subscription)
        } else {
            repository.subscribe(subscription)
        }
    }
}
