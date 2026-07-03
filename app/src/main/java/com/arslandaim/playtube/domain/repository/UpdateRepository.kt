package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.domain.model.UpdateInfo

interface UpdateRepository {
    suspend fun getLatestUpdate(): Result<UpdateInfo?>
}
