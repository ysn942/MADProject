package com.example.myapplication.room

import androidx.room.*

@Dao
interface ICoordinatesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CoordinatesEntity)

    @Query("SELECT * FROM coordinates ORDER BY timestamp DESC")
    suspend fun getAll(): List<CoordinatesEntity>

    @Query("DELETE FROM coordinates WHERE timestamp = :timestamp")
    suspend fun deleteByTimestamp(timestamp: Long)

    @Update
    suspend fun update(entity: CoordinatesEntity)
}
