package com.arslandaim.playtube.domain.model

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String?,
    val downloadUrl: String,
    val publishedAt: String?
)
