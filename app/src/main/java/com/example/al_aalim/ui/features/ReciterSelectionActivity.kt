package com.example.al_aalim.ui.features

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.SubcomposeAsyncImage
import com.example.al_aalim.R
import com.example.al_aalim.model.Reciter
import com.example.al_aalim.repository.SettingsRepository
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.components.AdaptiveContainer

// Reciter photos from Islamic audio CDN / Wikipedia
private val reciterPhotoMap = mapOf(
    "ar.alafasy"           to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Mishary_Alafasy.jpg/800px-Mishary_Alafasy.jpg",
    "ar.abdurrahmansudais" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/AbdurrahmanAsSudais.jpg/800px-AbdurrahmanAsSudais.jpg",
    "ar.saudalshuraim"     to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/SaudAlShuraim.jpg/800px-SaudAlShuraim.jpg",
    "ar.mahermuaiqly"      to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Maher_Al-Muaiqly.jpg/800px-Maher_Al-Muaiqly.jpg",
    "ar.minshawi"          to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/39/Muhammad_Siddiq_Minshawi.jpg/800px-Muhammad_Siddiq_Minshawi.jpg",
    "ar.husary"            to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Husary.jpg/800px-Husary.jpg",
    "ar.abdulsamad"        to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Abdul_Basit_%27Abd_us-Samad.jpg/800px-Abdul_Basit_%27Abd_us-Samad.jpg",
    "ar.haniarrifai"       to "https://everyayah.com/data/Hani_Rifai_192kbps/cover.jpg",
    "ar.ahmedajamy"        to "https://upload.wikimedia.org/wikipedia/commons/e/e5/Ahmad_bin_Ali_Al-Ajmi.png"
)

class ReciterSelectionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val repository = SettingsRepository(this)

        setContent {
            AlAalimTheme {
                val currentReciterId by repository.getSelectedReciterId().collectAsState(initial = "ar.alafasy")
                var searchQuery by remember { mutableStateOf("") }

                val allReciters = remember {
                    listOf(
                        Reciter("ar.alafasy",           "Mishary Rashid Alafasy",        "Murattal", "High Quality"),
                        Reciter("ar.abdurrahmansudais", "Abdurrahman As-Sudais",          "Murattal", "Haramain"),
                        Reciter("ar.saudalshuraim",     "Saud Al-Shuraim",               "Murattal", "Haramain"),
                        Reciter("ar.mahermuaiqly",      "Maher Al-Muaiqly",              "Murattal", "Classic"),
                        Reciter("ar.minshawi",          "Mohamed Siddiq Al-Minshawi",    "Murattal", "Classic"),
                        Reciter("ar.husary",            "Mahmoud Khalil Al-Husary",      "Murattal", "Classic"),
                        Reciter("ar.abdulsamad",        "Abdul Basit Abdul Samad",       "Murattal", "Mujawwad Style"),
                        Reciter("ar.haniarrifai",       "Hani Ar-Rifai",                 "Murattal", "Emotional"),
                        Reciter("ar.ahmedajamy",        "Ahmed Al-Ajamy",                "Murattal", "High Quality")
                    )
                }

                val filteredReciters = allReciters.filter {
                    it.nameEnglish.contains(searchQuery, ignoreCase = true)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    AdaptiveContainer {
                        Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                        .background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Gold, RoundedCornerShape(16.dp))
                                    .background(Color(0xFF233F40))
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Text("Select Reciter", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Box(modifier = Modifier.size(44.dp))
                        }

                        // Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search reciter...", color = Color.White.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gold) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(filteredReciters) { reciter ->
                                val isSelected = reciter.id == currentReciterId
                                ReciterCard(
                                    reciter = reciter,
                                    isSelected = isSelected,
                                    photoUrl = reciterPhotoMap[reciter.id],
                                    onClick = {
                                        repository.setSelectedReciterId(reciter.id)
                                        repository.quranReciter = reciter.nameEnglish
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ReciterCard(
    reciter: Reciter,
    isSelected: Boolean,
    photoUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (isSelected) Gold.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with photo or initial fallback
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape)
                .background(Color(0x33D4A843)),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                SubcomposeAsyncImage(
                    model = photoUrl,
                    contentDescription = reciter.nameEnglish,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    error = {
                        // Offline fallback: show initials
                        Text(
                            text = reciter.nameEnglish.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                            color = Gold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            } else {
                Text(
                    text = reciter.nameEnglish.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                    color = Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(reciter.nameEnglish, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${reciter.style} • ${reciter.quality}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Gold, unselectedColor = Color.White.copy(alpha = 0.4f))
        )
    }
}
