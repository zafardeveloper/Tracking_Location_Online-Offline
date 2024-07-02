package com.example.tracking_location.room

import androidx.lifecycle.LiveData

class LocationRepository(private val locationDao: LocationDao) {
    val getLocation: LiveData<LocationEntity> = locationDao.getLocation()

    suspend fun insertLocation(location: LocationEntity) = locationDao.insertLocation(location)
}