/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM download_missions")
    fun getAllMissions(): Flow<List<DownloadMissionEntity>>

    @Query("SELECT * FROM download_missions WHERE id = :missionId")
    suspend fun getMissionById(missionId: Long): DownloadMissionEntity?

    @Query("SELECT * FROM download_missions WHERE videoId = :videoId")
    suspend fun getMissionByVideoId(videoId: String): DownloadMissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: DownloadMissionEntity): Long

    @Update
    suspend fun updateMission(mission: DownloadMissionEntity)

    @Delete
    suspend fun deleteMission(mission: DownloadMissionEntity)

    @Query("UPDATE download_missions SET status = :status WHERE id = :missionId")
    suspend fun updateStatus(missionId: Long, status: MissionStatus)

    @Query("UPDATE download_missions SET downloadedBytes = :downloadedBytes WHERE id = :missionId")
    suspend fun updateProgress(missionId: Long, downloadedBytes: Long)

    @Transaction
    @Query("SELECT * FROM download_missions WHERE id = :missionId")
    suspend fun getMissionWithChunks(missionId: Long): MissionWithChunks?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: DownloadChunkEntity): Long

    @Update
    suspend fun updateChunk(chunk: DownloadChunkEntity)

    @Query("SELECT * FROM download_chunks WHERE missionId = :missionId")
    suspend fun getChunksForMission(missionId: Long): List<DownloadChunkEntity>

    @Query("UPDATE download_chunks SET bytesDownloaded = :bytes, isCompleted = :completed WHERE id = :chunkId")
    suspend fun updateChunkProgress(chunkId: Long, bytes: Long, completed: Boolean)
}

data class MissionWithChunks(
    @Embedded val mission: DownloadMissionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "missionId"
    )
    val chunks: List<DownloadChunkEntity>
)
