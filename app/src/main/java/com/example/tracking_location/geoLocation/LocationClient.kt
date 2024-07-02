package com.example.tracking_location.geoLocation

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long, useOnline: Boolean): Flow<Location>

    class LocationException(message: String): Exception()
}