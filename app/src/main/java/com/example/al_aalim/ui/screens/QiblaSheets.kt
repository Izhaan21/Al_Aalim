package com.example.al_aalim.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.al_aalim.R
import com.example.al_aalim.data.CountryLocation
import com.example.al_aalim.ui.theme.PrimaryTeal
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionSheet(
    onDismissRequest: () -> Unit,
    onLocationSelected: (CountryLocation) -> Unit,
    onUseMyLocation: () -> Unit,
    searchLocations: (String) -> List<CountryLocation>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var searchQuery by remember { mutableStateOf("") }
    var locationResults by remember { mutableStateOf<List<CountryLocation>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<CountryLocation?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            locationResults = emptyList()
            selectedLocation = null
            isSearching = false
        } else {
            isSearching = true
            delay(300) // Debounce
            locationResults = searchLocations(searchQuery)
            selectedLocation = null
            isSearching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Location",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search country or city...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else if (searchQuery.isNotEmpty() && locationResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No locations found", color = Color.Gray)
                }
            } else if (locationResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 300.dp)
                ) {
                    items(locationResults) { location ->
                        val isSelected = selectedLocation == location
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFFF3F4F6) else Color.White)
                                .clickable { selectedLocation = location }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = location.flag,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "${location.name}, ${location.capital}",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = "Selected",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        selectedLocation?.let {
                            onLocationSelected(it)
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = selectedLocation != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Choose", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = {
                    onUseMyLocation()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use My Location Instead", color = PrimaryTeal, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuidanceSheet(
    onDismissRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Enable Location Services",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Location services are currently turned off.\n\nTo use Qibla direction, please:\n1. Tap 'Open Settings'\n2. Turn on 'Location' or 'GPS'\n3. Return to Al-Aalim",
                fontSize = 15.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    onOpenSettings()
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
