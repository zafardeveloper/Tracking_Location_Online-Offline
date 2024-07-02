package com.example.tracking_location.geoLocation

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.tracking_location.R
import com.example.tracking_location.room.LocationDao
import com.example.tracking_location.room.LocationDatabase
import com.example.tracking_location.room.LocationEntity
import com.example.tracking_location.room.LocationRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocationService : Service() {

    private lateinit var locationClient: LocationClient
    private lateinit var locationDatabase: LocationDatabase
    private lateinit var locationRepository: LocationRepository
    private lateinit var locationDao: LocationDao
    private var useOnline = true

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        locationDatabase = LocationDatabase.getDatabase(applicationContext)
        locationDao = locationDatabase.locationDao()
        locationRepository = LocationRepository(locationDao)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                useOnline = intent.getBooleanExtra(EXTRA_USE_ONLINE, true)
                start()
            }

            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking Location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(10 * 1000L, useOnline)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val latitude = location.latitude.toString()
                val longitude = location.longitude.toString()
                val timestamp = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val formattedTime = timestamp.format(formatter)

                val locationEntity = LocationEntity(
                    latitude = latitude.toDouble(),
                    longitude = longitude.toDouble(),
                    timestamp = formattedTime
                )

                serviceScope.launch {
                    locationRepository.insertLocation(locationEntity)
                }

                val updatedNotification = notification.setContentText(
                    "Location: ($latitude, $longitude)"
                )
                if (useOnline) {
                    Log.d("MyLog", "Location Online: ($latitude, $longitude)")
                } else {
                    Log.d("MyLog", "Location Offline: ($latitude, $longitude)")
                }
                notificationManager.notify(1, updatedNotification.build())
            }.launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_USE_ONLINE = "EXTRA_USE_ONLINE"
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}