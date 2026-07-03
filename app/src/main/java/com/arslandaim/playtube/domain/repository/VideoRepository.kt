package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.model.PlaylistDetails
import com.arslandaim.playtube.domain.model.StreamBundle

interface VideoRepository {
    suspend fun getStreamBundle(videoId: String): StreamBundle
    suspend fun getChannelDetails(channelUrl: String): ChannelDetails
    suspend fun getPlaylistDetails(playlistUrl: String): PlaylistDetails
}
