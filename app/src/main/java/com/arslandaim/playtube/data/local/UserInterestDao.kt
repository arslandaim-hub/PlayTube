/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*

@Dao
interface UserInterestDao {
    @Query("SELECT * FROM user_interests ORDER BY weight DESC LIMIT :limit")
    suspend fun getTopInterests(limit: Int): List<UserInterestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(interest: UserInterestEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(interests: List<UserInterestEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllIgnoreSync(interests: List<UserInterestEntity>)

    @Query("SELECT * FROM user_interests")
    suspend fun getAllInterestsStatic(): List<UserInterestEntity>

    @Query("DELETE FROM user_interests")
    fun clearInterests()

    @Query("UPDATE user_interests SET weight = weight * :decayFactor")
    suspend fun applyDecay(decayFactor: Float)

    @Query("SELECT * FROM user_interests WHERE keyword = :keyword")
    suspend fun getInterest(keyword: String): UserInterestEntity?

    @Query("DELETE FROM user_interests WHERE weight < :threshold")
    suspend fun purgeLowInterests(threshold: Float = 0.1f)
    
    @Query("SELECT COUNT(*) FROM user_interests")
    suspend fun getInterestsCount(): Int
}
