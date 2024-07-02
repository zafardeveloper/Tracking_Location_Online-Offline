package com.example.tracking_location.geoLocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.example.tracking_location.geoLocation.offline.OfflineLocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
    ) : LocationClient {
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long, useOnline: Boolean): Flow<Location> {

        return if (useOnline) {
            callbackFlow {
                if (!context.hasLocationPermission()) {
                    throw LocationClient.LocationException("Missing Location Permission!")
                }

                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isGpsEnabled && !isNetworkEnabled) {
                    throw LocationClient.LocationException("GPS is disabled")
                }

                val request = LocationRequest.Builder(interval)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(interval)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)
                        result.locations.lastOrNull()?.let { location ->
                            trySend(location)
                        }
                    }
                }

                client.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )

                awaitClose {
                    client.removeLocationUpdates(locationCallback)
                }
            }
        } else {
            OfflineLocationClient(context).getLocationUpdates(interval = interval, useOnline = false)
        }
    }
}