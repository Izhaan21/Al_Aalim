package com.example.al_aalim.ui.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.al_aalim.R
import com.example.al_aalim.model.Surah
import com.example.al_aalim.ui.main.BottomNavBar
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.components.AdaptiveContainer
import com.example.al_aalim.utils.LanguageManager
import com.example.al_aalim.viewmodel.QuranViewModel

class QuranActivity : ComponentActivity() {

    private lateinit var viewModel: QuranViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        viewModel = ViewModelProvider(this)[QuranViewModel::class.java]

        setContent {
            AlAalimTheme {
                QuranScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onNavigate = { tab ->
                        when(tab) {
                            0 -> finish()
                            1 -> { startActivity(Intent(this, QiblaActivity::class.java)); finish() }
                            2 -> { /* already here */ }
                            3 -> { startActivity(Intent(this, StoreActivity::class.java)); finish() }
                        }
                    },
                    onSurahClick = { surah ->
                        val intent = Intent(this, SurahReaderActivity::class.java).apply {
                            putExtra(SurahReaderActivity.EXTRA_SURAH_NUMBER, surah.number)
                            putExtra(SurahReaderActivity.EXTRA_SURAH_NAME, surah.englishName)
                            putExtra(SurahReaderActivity.EXTRA_SURAH_MEANING, surah.meaning)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}

@Composable
fun QuranScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSurahClick: (Surah) -> Unit,
    showBottomNav: Boolean = true
) {
    val selectedTab = remember { mutableStateOf(2) }
    
    val displayedSurahs by viewModel.displayedSurahs.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val isFavsActive by viewModel.isFavouritesTabActive.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
            .statusBarsPadding()
    ) {
        AdaptiveContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
            // Top Bar (Home Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .border(1.dp, Gold, RoundedCornerShape(30.dp))
                        .background(Color(0xFF233F40))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text("Quran", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Box(
                    modifier = Modifier.size(44.dp) // Maintain balance
                )
            }

            // Search Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.setSearchQuery(it)
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search surah name or number...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                searchQuery = ""
                                viewModel.setSearchQuery("")
                            }
                    )
                }
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (!isFavsActive) Brush.linearGradient(listOf(Gold, Color(0xFFC59A45)))
                            else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.05f))
                        )
                        .clickable { viewModel.setTab(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "All",
                        color = if (!isFavsActive) Color.White else Color.White.copy(alpha = 0.8f),
                        fontWeight = if (!isFavsActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isFavsActive) Brush.linearGradient(listOf(Gold, Color(0xFFC59A45)))
                            else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.05f))
                        )
                        .clickable { viewModel.setTab(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Favorites",
                        color = if (isFavsActive) Color.White else Color.White.copy(alpha = 0.8f),
                        fontWeight = if (isFavsActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }

            // Surahs List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
            ) {
                items(displayedSurahs, key = { it.number }) { surah ->
                    val isFav = favourites.contains(surah.number)
                    SurahItem(
                        surah = surah,
                        isFavourite = isFav,
                        onClick = { onSurahClick(surah) },
                        onToggleFav = { viewModel.toggleFavourite(surah.number) }
                    )
                }
            }
        }

        // Bottom Nav
        if (showBottomNav) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNavBar(
                    selectedTab = selectedTab.value,
                    onTabSelected = {
                        selectedTab.value = it
                        onNavigate(it)
                    }
                )
            }
        }
    }
}
}

@Composable
fun SurahItem(
    surah: Surah,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Badge
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SURAH", color = Gold, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("${surah.number}", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.englishName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Arabic Name
            Text(
                text = surah.arabicName,
                color = Gold,
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Favourite Icon
            Icon(
                imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favourite",
                tint = if (isFavourite) Color.Red else Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggleFav() }
                    .padding(2.dp)
            )
        }
    }
}
