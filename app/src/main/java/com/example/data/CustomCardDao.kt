package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCardDao {
    @Query("SELECT * FROM custom_cards ORDER BY id DESC")
    fun getAllCustomCards(): Flow<List<CustomCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCard(card: CustomCardEntity)

    @Delete
    suspend fun deleteCustomCard(card: CustomCardEntity)
}
