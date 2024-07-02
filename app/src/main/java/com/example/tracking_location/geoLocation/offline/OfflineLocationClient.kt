package com.example.tracking_location.geoLocation.offline

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.tracking_location.geoLocation.LocationClient
import com.example.tracking_location.geoLocation.hasLocationPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class OfflineLocationClient(private val context: Context) : LocationClient {

    override fun getLocationUpdates(interval: Long, useOnline: Boolean): Flow<Location> {
        return callbackFlow {
            if (!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing Location Permission!")
            }

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (!isGpsEnabled) {
                throw LocationClient.LocationException("GPS is disabled")
            }

            val locationListener = object : LocationListener {

                override fun onLocationChanged(location: Location) {
                    trySend(location).isSuccess
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@callbackFlow
            }

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                interval,
                0f,
                locationListener,
                Looper.getMainLooper()
            )

            awaitClose {
                locationManager.removeUpdates(locationListener)
            }
        }
    }
}