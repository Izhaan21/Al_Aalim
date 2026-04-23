package com.example.al_aalim.ui.screens

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.al_aalim.R
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.PrimaryTeal
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape

@Composable
fun QiblaScreen(
    mapView: View?,
    compassRotation: Float,
    angleToQibla: Int,
    distanceKm: Double,
    latitude: Double,
    longitude: Double,
    isPhoneFlat: Boolean,
    hasLocation: Boolean,
    isLocationEnabled: Boolean,
    onBackClick: () -> Unit,
    onExpandMapClick: () -> Unit,
    onUseMyLocationClick: () -> Unit
) {
    val qiblaFound = angleToQibla == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Gold, RoundedCornerShape(16.dp))
                        .background(Color(0xFF233F40))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Qibla",
                        color = Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(44.dp)) // To balance the back button
            }

            // Map Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (mapView != null) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onExpandMapClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Expand Map",
                        tint = Color.White,
                        modifier = Modifier
                            .rotate(135f)
                            .size(16.dp)
                    )
                }
            }

            // Distance and Angle Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = String.format("%.1fKM", distanceKm),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distance from Mecca",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "${angleToQibla}°",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Device's Angle to Qibla",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            // Compass Container
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Fixed Outer Ring
                Image(
                    painter = painterResource(id = R.drawable.compass_outer_ring),
                    contentDescription = null,
                    modifier = Modifier.size(260.dp),
                    colorFilter = ColorFilter.tint(if (qiblaFound && isLocationEnabled) Gold else PrimaryTeal)
                )

                // Fixed Top Triangle
                Image(
                    painter = painterResource(id = R.drawable.ic_compass_triangle),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                        .size(20.dp, 14.dp),
                    colorFilter = ColorFilter.tint(if (qiblaFound && isLocationEnabled) Gold else PrimaryTeal)
                )

                // Rotating Inner Compass
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .rotate(if (hasLocation && isLocationEnabled) compassRotation else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.compass_middle_circle),
                        contentDescription = null,
                        modifier = Modifier.size(230.dp)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.compass_inner_circle),
                        contentDescription = null,
                        modifier = Modifier.size(190.dp)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.compass_needle),
                        contentDescription = null,
                        modifier = Modifier
                            .size(110.dp, 150.dp)
                            .rotate(180f) // from XML
                    )

                    // Counter-rotated Kaaba Icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_kaaba),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 12.dp)
                            .size(48.dp)
                            .rotate(if (hasLocation && isLocationEnabled) -compassRotation else 0f)
                    )
                }
            }

            // Guidance and Info Texts
            val guidanceText = if (!hasLocation || !isLocationEnabled) {
                "Tap 'Use My Location' to find Qibla"
            } else {
                when {
                    angleToQibla == 0 -> "Qibla found! ✓"
                    angleToQibla <= 5 -> "Almost there!"
                    else -> "Rotate ${angleToQibla}° to align" // The exact direction relies on signed difference, we use absolute here and simplified text or we can pass the string.
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = guidanceText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    val latStr = String.format(java.util.Locale.US, "%.4f° %s", Math.abs(latitude), if (latitude >= 0) "N" else "S")
                    val lonStr = String.format(java.util.Locale.US, "%.4f° %s", Math.abs(longitude), if (longitude >= 0) "E" else "W")
                    Text(
                        text = "$latStr, $lonStr",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "May Allah accept your prayers",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (!isPhoneFlat && hasLocation && isLocationEnabled) {
                    Text(
                        text = "Please lay your phone flat",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!hasLocation || !isLocationEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onUseMyLocationClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Use My Location",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
