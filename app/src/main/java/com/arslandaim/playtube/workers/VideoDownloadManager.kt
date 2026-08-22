/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.workers

import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.utils.PTLog
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoDownloadManager @Inject constructor(
    private val downloadDao: DownloadDao
) {
    private val missionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeMissions = ConcurrentHashMap<String, Job>()

    fun isMissionActive(videoId: String): Boolean = activeMissions.containsKey(videoId)

    suspend fun runMission(videoId: String, block: suspend () -> Unit) {
        val job = kotlin.coroutines.coroutineContext[Job] ?: return
        activeMissions[videoId] = job
        try {
            block()
        } finally {
            activeMissions.remove(videoId)
        }
    }

    fun pauseMission(videoId: String) {
        PTLog.d("VideoDownloadManager", "Pausing mission $videoId")
        activeMissions[videoId]?.cancel()
        activeMissions.remove(videoId)
        missionScope.launch {
            downloadDao.setDownloadStatus(videoId, DownloadStatus.PAUSED)
        }
    }
}
