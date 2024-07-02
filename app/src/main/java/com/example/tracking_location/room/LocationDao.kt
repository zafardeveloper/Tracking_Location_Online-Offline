package com.example.tracking_location.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location LIMIT 1")
    fun getLocation(): LiveData<LocationEntity>

    @Query("SELECT * FROM location ORDER BY timestamp DESC")
    fun getAllLocations(): LiveData<List<LocationEntity>>
}