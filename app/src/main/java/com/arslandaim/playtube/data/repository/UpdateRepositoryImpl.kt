/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.UpdateInfo
import com.arslandaim.playtube.domain.repository.UpdateRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : UpdateRepository {

    override suspend fun getLatestUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/arslandaim-hub/PlayTube/releases/latest")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("GitHub API error: ${response.code}"))

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val json = JSONObject(body)

                val versionName = json.optString("tag_name", "").removePrefix("v")
                val releaseNotes = json.optString("body", "")
                val downloadUrl = json.optString("html_url", "")
                val publishedAt = json.optString("published_at", "")

                if (versionName.isBlank()) return@withContext Result.success(null)

                Result.success(
                    UpdateInfo(
                        versionName = versionName,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        publishedAt = publishedAt
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
