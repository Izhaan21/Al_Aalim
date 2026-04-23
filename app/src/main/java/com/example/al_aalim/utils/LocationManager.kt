package com.example.al_aalim.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Singleton to manage location across the app.
 * Pre-fetches location when app starts for instant Qibla display.
 */
object LocationManager {
    private const val TAG = "LocationManager"
    
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    
    // Cached location data
    var userLatitude: Double = 0.0
        private set
    var userLongitude: Double = 0.0
        private set
    var hasLocation: Boolean = false
        private set
    
    // Listeners for location updates
    private val locationListeners = mutableListOf<(Double, Double) -> Unit>()
    
    fun initialize(context: Context) {
        if (fusedLocationClient != null) return // Already initialized
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        Log.d(TAG, "LocationManager initialized")
        
        // Start fetching location if permission granted
        if (hasLocationPermission(context)) {
            startLocationUpdates(context)
        }
    }
    
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
    
    fun startLocationUpdates(context: Context) {
        if (!hasLocationPermission(context)) {
            Log.d(TAG, "No location permission")
            return
        }
        
        Log.d(TAG, "Starting location updates")
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
        
        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                context.mainLooper
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
        }
    }
    
    private fun updateLocation(location: Location) {
        Log.d(TAG, "Location received: ${location.latitude}, ${location.longitude}")
        userLatitude = location.latitude
        userLongitude = location.longitude
        hasLocation = true
        
        // Notify all listeners
        locationListeners.forEach { listener ->
            listener(userLatitude, userLongitude)
        }
    }
    
    fun addLocationListener(listener: (Double, Double) -> Unit) {
        locationListeners.add(listener)
        // If we already have location, notify immediately
        if (hasLocation) {
            listener(userLatitude, userLongitude)
        }
    }
    
    fun removeLocationListener(listener: (Double, Double) -> Unit) {
        locationListeners.remove(listener)
    }
    
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }
}
