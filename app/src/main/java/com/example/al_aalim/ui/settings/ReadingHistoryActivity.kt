package com.example.al_aalim.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.al_aalim.R
import com.example.al_aalim.model.ReadingHistoryItem
import com.example.al_aalim.repository.UserDataRepository
import com.example.al_aalim.ui.components.ConfirmationDialog
import com.example.al_aalim.ui.components.ReadingHistoryItemRow
import com.example.al_aalim.ui.features.SurahReaderActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.google.firebase.auth.FirebaseAuth

class ReadingHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = androidx.lifecycle.ViewModelProvider(
            this,
            com.example.al_aalim.viewmodel.ViewModelFactory(this)
        )[com.example.al_aalim.viewmodel.AccountViewModel::class.java]

        setContent {
            AlAalimTheme {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    viewModel.loadReadingHistory()
                }
                
                val history = viewModel.readingHistory.collectAsState().value

                val showClearDialog = androidx.compose.runtime.remember { 
                    mutableStateOf(false)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
                        .statusBarsPadding()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Gold, RoundedCornerShape(16.dp))
                                    .background(Color(0xFF233F40))
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Text("Reading History", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (history.isNotEmpty()) {
                                IconButton(onClick = { showClearDialog.value = true }) {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear All", tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(44.dp))
                            }
                        }

                        if (history.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No reading history yet", color = Color.White.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(history) { historyItem ->
                                    ReadingHistoryItemRow(
                                        surahNumber = historyItem.surahNumber,
                                        surahName = historyItem.surahName,
                                        surahMeaning = historyItem.surahMeaning,
                                        lastVerseNumber = historyItem.lastVerseNumber,
                                        timeAgo = "Recently",
                                        onClick = {
                                            val intent = Intent(this@ReadingHistoryActivity, SurahReaderActivity::class.java).apply {
                                                putExtra(SurahReaderActivity.EXTRA_SURAH_NUMBER, historyItem.surahNumber)
                                                putExtra(SurahReaderActivity.EXTRA_SURAH_NAME, historyItem.surahName)
                                                putExtra(SurahReaderActivity.EXTRA_SURAH_MEANING, historyItem.surahMeaning)
                                                putExtra(SurahReaderActivity.EXTRA_SCROLL_TO_VERSE, historyItem.lastVerseNumber)
                                            }
                                            startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }

                        if (showClearDialog.value) {
                            ConfirmationDialog(
                                title = "Clear History",
                                message = "Are you sure you want to delete all reading history?",
                                confirmText = "Clear",
                                cancelText = "Cancel",
                                onConfirm = {
                                    viewModel.clearReadingHistory()
                                    Toast.makeText(this@ReadingHistoryActivity, "Reading history cleared", Toast.LENGTH_SHORT).show()
                                    showClearDialog.value = false
                                },
                                onDismiss = { showClearDialog.value = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
