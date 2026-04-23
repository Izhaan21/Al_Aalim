package com.example.al_aalim.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class QiblaViewModel : ViewModel() {

    private val MECCA_LATITUDE = 21.4225
    private val MECCA_LONGITUDE = 39.8262

    // State holding user location and derived Qibla bearing
    private val _userLocation = MutableStateFlow<LocationData?>(null)
    val userLocation: StateFlow<LocationData?> = _userLocation.asStateFlow()

    fun updateUserLocation(latitude: Double, longitude: Double) {
        val bearing = calculateBearing(latitude, longitude, MECCA_LATITUDE, MECCA_LONGITUDE)
        val distance = calculateDistance(latitude, longitude, MECCA_LATITUDE, MECCA_LONGITUDE)
        
        _userLocation.value = LocationData(
            latitude = latitude,
            longitude = longitude,
            qiblaBearing = bearing.toFloat(),
            distanceToMeccaKm = distance,
            meccaLatitude = MECCA_LATITUDE,
            meccaLongitude = MECCA_LONGITUDE
        )
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

        var bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val qiblaBearing: Float,
    val distanceToMeccaKm: Double,
    val meccaLatitude: Double,
    val meccaLongitude: Double
)
