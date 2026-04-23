package com.example.al_aalim.ui.features

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.al_aalim.R
import com.example.al_aalim.repository.UserDataRepository
import com.example.al_aalim.repository.SettingsRepository
import com.example.al_aalim.viewmodel.SettingsViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.example.al_aalim.utils.LanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import java.io.File
import androidx.lifecycle.ViewModelProvider
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.components.AdaptiveContainer

data class Verse(
    val numberInSurah: Int,
    val arabicText: String,
    val translation: String
)

enum class DisplayMode { ARABIC, TRANSLATION, BOTH }

class SurahReaderActivity : ComponentActivity() {

    private lateinit var userDataRepository: UserDataRepository
    private lateinit var settingsViewModel: SettingsViewModel

    private var mediaPlayer: MediaPlayer? = null
    private var activePlayJob: Job? = null
    private var prefetchJob: Job? = null

    // Compose States
    private val versesState = mutableStateOf<List<Verse>>(emptyList())
    private val isPlayingState = mutableStateOf(false)
    private val currentVerseIndexState = mutableIntStateOf(-1)
    private val isLoadingState = mutableStateOf(true)

    private val displayModeState = mutableStateOf(DisplayMode.BOTH)
    private val fontSizeState = mutableFloatStateOf(22f)
    private val quranScriptState = mutableStateOf("Indopak")
    
    // Set of bookmarked verse numbers
    private val bookmarkedVersesState = mutableStateOf(setOf<Int>())
    // Reactive reciter — observed live from repo so it updates immediately after ReciterSelectionActivity finishes
    private lateinit var settingsRepo: SettingsRepository

    // Tracks the verse number (1-based) that is currently visible at the top of the list
    private var currentReadingVerse = 1

    // Intent payload
    private var surahNumber = 1
    private var surahName = "Al-Fatihah"
    private var surahMeaning = "The Opening"
    private var scrollToVerse = -1

    private val gson = Gson()

    // Keys match the IDs stored by SettingsRepository (ar.* format)
    private val reciterFolderMap = mapOf(
        "ar.alafasy"           to "Alafasy_128kbps",
        "ar.abdurrahmansudais" to "Abdurrahmaan_As-Sudais_192kbps",
        "ar.saudalshuraim"     to "Saood_ash-Shuraym_128kbps",
        "ar.mahermuaiqly"      to "MaherAlMuaiqly128kbps",
        "ar.husary"            to "Husary_128kbps",
        "ar.minshawi"          to "Minshawy_Murattal_128kbps",
        "ar.abdulsamad"        to "Abdul_Basit_Murattal_64kbps",
        "ar.haniarrifai"       to "Hani_Rifai_192kbps",
        "ar.ahmedajamy"        to "ahmed_ibn_ali_al_ajamy_128kbps"
    )

    companion object {
        const val EXTRA_SURAH_NUMBER = "surah_number"
        const val EXTRA_SURAH_NAME = "surah_name"
        const val EXTRA_SURAH_MEANING = "surah_meaning"
        const val EXTRA_SCROLL_TO_VERSE = "scroll_to_verse"
        const val REQUEST_RECITER = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        surahNumber = intent.getIntExtra(EXTRA_SURAH_NUMBER, 1)
        surahName = intent.getStringExtra(EXTRA_SURAH_NAME) ?: "Al-Fatihah"
        surahMeaning = intent.getStringExtra(EXTRA_SURAH_MEANING) ?: "The Opening"
        scrollToVerse = intent.getIntExtra(EXTRA_SCROLL_TO_VERSE, -1)

        val factory = ViewModelFactory(this)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        settingsRepo = SettingsRepository(this)

        quranScriptState.value = settingsViewModel.quranScript.value

        userDataRepository = UserDataRepository(this)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Record open — start from the requested verse or verse 1
            val initialVerse = if (scrollToVerse > 0) scrollToVerse else 1
            currentReadingVerse = initialVerse
            userDataRepository.addReadingHistory(userId, surahNumber, surahName, surahMeaning, initialVerse)

            // Load bookmarks for this surah
            val saved = userDataRepository.getBookmarks(userId).filter { it.surahNumber == surahNumber }.map { it.verseNumber }
            bookmarkedVersesState.value = saved.toSet()
        }

        loadVerses()

        setContent {
            val verses by versesState
            val isPlaying by isPlayingState
            val currentVerseIndex by currentVerseIndexState
            val isLoading by isLoadingState
            
            val displayMode by displayModeState
            val fontSize by fontSizeState
            val quranScript by quranScriptState
            val bookmarkedVerses by bookmarkedVersesState
            // Reactively collect reciter name from repo — updates live when user returns from selection
            val reciterName by settingsRepo.getSelectedReciterId()
                .map { id ->
                    settingsRepo.quranReciter.ifBlank {
                        when (id) {
                            "ar.alafasy"           -> "Mishary Rashid Alafasy"
                            "ar.abdurrahmansudais" -> "Abdurrahman As-Sudais"
                            "ar.saudalshuraim"     -> "Saud Al-Shuraim"
                            "ar.mahermuaiqly"      -> "Maher Al-Muaiqly"
                            "ar.minshawi"          -> "Mohamed Siddiq Al-Minshawi"
                            "ar.husary"            -> "Mahmoud Khalil Al-Husary"
                            "ar.abdulsamad"        -> "Abdul Basit Abdul Samad"
                            "ar.haniarrifai"       -> "Hani Ar-Rifai"
                            "ar.ahmedajamy"        -> "Ahmed Al-Ajamy"
                            else -> settingsRepo.quranReciter
                        }
                    }
                }
                .collectAsState(initial = settingsRepo.quranReciter)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading $surahName...", color = Color.White)
                    }
                }
            } else {
                SurahReaderScreen(
                    verses = verses,
                    surahNumber = surahNumber,
                    surahName = surahName,
                    surahMeaning = surahMeaning,
                    displayMode = displayMode,
                    fontSize = fontSize,
                    quranScript = quranScript,
                    reciterName = reciterName,
                    isPlaying = isPlaying,
                    playingIndex = currentVerseIndex,
                    bookmarkedVerses = bookmarkedVerses,
                    requestScrollToIndex = if (scrollToVerse != -1) scrollToVerse - 1 else -1,
                    onBack = { finish() },
                    onDisplayModeChange = { displayModeState.value = it },
                    onFontSizeChange = { fontSizeState.floatValue = it },
                    onVersePlay = { index -> playSingleVerse(index) },
                    onVerseBookmark = { index -> toggleBookmark(index) },
                    onVerseShare = { verse -> shareVerse(verse) },
                    onNextVerse = { nextVerse() },
                    onPrevVerse = { prevVerse() },
                    onPlayPause = { if (isPlaying) pauseAudio() else resumeOrPlay() },
                    onChangeReciter = { openReciterSelection() },
                    onScrollToVerseConsumed = { scrollToVerse = -1 },
                    onReadingVerseChanged = { verseNumber ->
                        currentReadingVerse = verseNumber
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAudio()
        // Persist the reading position so history shows the correct verse
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && versesState.value.isNotEmpty()) {
            userDataRepository.addReadingHistory(
                userId, surahNumber, surahName, surahMeaning, currentReadingVerse
            )
        }
    }

    override fun onResume() {
        super.onResume()
        quranScriptState.value = settingsViewModel.quranScript.value
    }

    override fun onDestroy() {
        super.onDestroy()
        activePlayJob?.cancel()
        prefetchJob?.cancel()
        releasePlayer()
    }

    private fun loadVerses() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = assets.open("quran.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<SurahJson>>() {}.type
                val allSurahs = gson.fromJson<List<SurahJson>>(json, type)

                val surahData = allSurahs.firstOrNull { it.s == surahNumber }
                val builtVerses = surahData?.v?.map { v ->
                    Verse(numberInSurah = v.n, arabicText = v.a, translation = v.e)
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    versesState.value = builtVerses
                    isLoadingState.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingState.value = false
                    Toast.makeText(this@SurahReaderActivity, "Failed to load verses", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleBookmark(verseNumber: Int) {
        val currentSet = bookmarkedVersesState.value.toMutableSet()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (currentSet.contains(verseNumber)) {
            currentSet.remove(verseNumber)
            userId?.let { userDataRepository.removeBookmark(it, surahNumber, verseNumber) }
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            currentSet.add(verseNumber)
            val verse = versesState.value.firstOrNull { it.numberInSurah == verseNumber }
            if (userId != null && verse != null) {
                userDataRepository.addBookmark(userId, surahNumber, surahName, surahMeaning, verseNumber, verse.arabicText, verse.translation)
            }
            Toast.makeText(this, "Ayah $verseNumber bookmarked ❤️", Toast.LENGTH_SHORT).show()
        }
        bookmarkedVersesState.value = currentSet
    }

    private fun shareVerse(verse: Verse) {
        val shareText = buildString {
            append(verse.arabicText)
            append("\n\n")
            append("${verse.numberInSurah}. ${verse.translation}")
            append("\n\n— $surahName, Ayah ${verse.numberInSurah}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Verse"))
    }

    private fun openReciterSelection() {
        val intent = Intent(this, ReciterSelectionActivity::class.java)
        startActivityForResult(intent, REQUEST_RECITER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RECITER) {
            val freshPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
            val newId = freshPrefs.getString("selected_reciter", "ar.alafasy") ?: "ar.alafasy"
            val newName = freshPrefs.getString("quran_reciter", "Mishary Rashid Alafasy") ?: "Mishary Rashid Alafasy"
            
            // Force the local repo instance to emit the new ID so the UI collectAsState updates
            settingsRepo.setSelectedReciterId(newId)
            settingsRepo.quranReciter = newName
            
            settingsViewModel.setQuranReciter(newName)
            settingsViewModel.setQuranReciterId(newId)
            
            stopAndResetAudio()
        }
    }

    // --- Audio Controls ---

    private fun resumeOrPlay() {
        val player = mediaPlayer
        if (player != null && !isPlayingState.value) {
            player.start()
            isPlayingState.value = true
        } else {
            playVerseAtIndex(if (currentVerseIndexState.intValue >= 0) currentVerseIndexState.intValue else 0)
        }
    }

    private fun playSingleVerse(index: Int) {
        if (index == currentVerseIndexState.intValue && mediaPlayer != null) {
            if (isPlayingState.value) pauseAudio() else resumeOrPlay()
        } else {
            currentVerseIndexState.intValue = index
            playVerseAtIndex(index)
        }
    }

    private fun nextVerse() {
        if (currentVerseIndexState.intValue < versesState.value.size - 1) {
            currentVerseIndexState.intValue++
            playVerseAtIndex(currentVerseIndexState.intValue)
        } else {
            Toast.makeText(this, "Last verse", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prevVerse() {
        if (currentVerseIndexState.intValue > 0) {
            currentVerseIndexState.intValue--
            playVerseAtIndex(currentVerseIndexState.intValue)
        } else {
            Toast.makeText(this, "First verse", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlayingState.value = false
    }

    private fun stopAndResetAudio() {
        mediaPlayer?.run {
            try { if (isPlayingState.value) stop() } catch (_: Exception) {}
            reset()
            release()
        }
        mediaPlayer = null
        isPlayingState.value = false
    }

    private fun releasePlayer() {
        stopAndResetAudio()
    }

    private fun getLocalAudioFile(folder: String, surahStr: String, ayahStr: String): File {
        val dir = File(cacheDir, "quran_audio/$folder")
        dir.mkdirs()
        return File(dir, "$surahStr$ayahStr.mp3")
    }

    private fun downloadAudioFile(url: String, dest: File): Boolean {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.connect()
            if (conn.responseCode != 200) return false
            conn.inputStream.use { it.copyTo(dest.outputStream()) }
            dest.length() > 0
        } catch (e: Exception) {
            dest.delete()
            false
        }
    }

    private fun prefetchVerseAtIndex(index: Int) {
        if (index < 0 || index >= versesState.value.size) return
        val verse = versesState.value[index]
        val reciterId = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getString("selected_reciter", "ar.alafasy") ?: "ar.alafasy"
        val folder = reciterFolderMap[reciterId] ?: "Alafasy_128kbps"
        val surahStr = surahNumber.toString().padStart(3, '0')
        val ayahStr = verse.numberInSurah.toString().padStart(3, '0')
        val localFile = getLocalAudioFile(folder, surahStr, ayahStr)
        if (localFile.exists() && localFile.length() > 0) return

        prefetchJob?.cancel()
        prefetchJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val url = "https://everyayah.com/data/$folder/$surahStr$ayahStr.mp3"
            downloadAudioFile(url, localFile)
        }
    }

    private fun playFromLocalFile(file: File, index: Int) {
        if (isDestroyed) return
        stopAndResetAudio()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                this@SurahReaderActivity.isPlayingState.value = true
                prefetchVerseAtIndex(index + 1)

                setOnCompletionListener {
                    val next = index + 1
                    if (next < versesState.value.size) {
                        currentVerseIndexState.intValue = next
                        playVerseAtIndex(next)
                    } else {
                        this@SurahReaderActivity.isPlayingState.value = false
                        currentVerseIndexState.intValue = -1
                    }
                }

                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@SurahReaderActivity, "Playback error", Toast.LENGTH_SHORT).show()
                    this@SurahReaderActivity.isPlayingState.value = false
                    currentVerseIndexState.intValue = -1
                    false
                }
            } catch (e: Exception) {
                Toast.makeText(this@SurahReaderActivity, "Unable to play audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playVerseAtIndex(index: Int) {
        if (versesState.value.isEmpty() || index < 0 || index >= versesState.value.size) return

        val verse = versesState.value[index]
        val reciterId = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getString("selected_reciter", "ar.alafasy") ?: "ar.alafasy"
        val folder = reciterFolderMap[reciterId] ?: "Alafasy_128kbps"
        val surahStr = surahNumber.toString().padStart(3, '0')
        val ayahStr = verse.numberInSurah.toString().padStart(3, '0')
        val url = "https://everyayah.com/data/$folder/$surahStr$ayahStr.mp3"
        val localFile = getLocalAudioFile(folder, surahStr, ayahStr)

        stopAndResetAudio()
        activePlayJob?.cancel()

        if (localFile.exists() && localFile.length() > 0) {
            playFromLocalFile(localFile, index)
        } else {
            activePlayJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val ok = downloadAudioFile(url, localFile)
                withContext(Dispatchers.Main) {
                    if (ok && currentVerseIndexState.intValue == index) {
                        playFromLocalFile(localFile, index)
                    } else if (!ok) {
                        Toast.makeText(this@SurahReaderActivity, "Audio unavailable. Check internet connection.", Toast.LENGTH_SHORT).show()
                        currentVerseIndexState.intValue = -1
                        isPlayingState.value = false
                    }
                }
            }
        }
    }

    private data class SurahJson(val s: Int, val v: List<VerseJson>)
    private data class VerseJson(val n: Int, val a: String, val e: String)
    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(LanguageManager.applyLanguage(newBase)) }
}

@Composable
fun SurahReaderScreen(
    verses: List<Verse>,
    surahNumber: Int,
    surahName: String,
    surahMeaning: String,
    displayMode: DisplayMode,
    fontSize: Float,
    quranScript: String,
    reciterName: String,
    isPlaying: Boolean,
    playingIndex: Int,
    bookmarkedVerses: Set<Int>,
    requestScrollToIndex: Int,
    onBack: () -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onVersePlay: (Int) -> Unit,
    onVerseBookmark: (Int) -> Unit,
    onVerseShare: (Verse) -> Unit,
    onNextVerse: () -> Unit,
    onPrevVerse: () -> Unit,
    onPlayPause: () -> Unit,
    onChangeReciter: () -> Unit,
    onScrollToVerseConsumed: () -> Unit,
    onReadingVerseChanged: (Int) -> Unit = {}
) {
    val listState = rememberLazyListState()
    // Index of the verse that should blink once (-1 = none)
    var blinkTargetIndex by remember { mutableIntStateOf(-1) }
    // Alpha of the gold blink overlay (0f = invisible, 1f = full gold)
    val blinkAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    // The list index (0-based) of the topmost visible verse — used for the reading indicator
    var readingVerseIndex by remember { mutableIntStateOf(-1) }

    // Track which verse is currently visible at the top and report it back
    LaunchedEffect(listState, verses) {
        if (verses.isEmpty()) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }
            .collect { listIndex ->
                // Items 0..2 are header items; verse items start at index 3
                val verseListIndex = (listIndex - 3).coerceAtLeast(0)
                if (verseListIndex < verses.size) {
                    readingVerseIndex = verseListIndex
                    val verseNumber = verses[verseListIndex].numberInSurah
                    onReadingVerseChanged(verseNumber)
                }
            }
    }

    LaunchedEffect(requestScrollToIndex) {
        if (requestScrollToIndex >= 0 && requestScrollToIndex < verses.size) {
            listState.scrollToItem(requestScrollToIndex + 3) // Offset by header items
            // Trigger blink on the target verse
            blinkTargetIndex = requestScrollToIndex
            // Pulse: fade in gold → hold → fade out → done
            blinkAlpha.animateTo(0.45f, animationSpec = androidx.compose.animation.core.tween(300))
            blinkAlpha.animateTo(0f,    animationSpec = androidx.compose.animation.core.tween(250))
            blinkAlpha.animateTo(0.45f, animationSpec = androidx.compose.animation.core.tween(300))
            blinkAlpha.animateTo(0f,    animationSpec = androidx.compose.animation.core.tween(400))
            blinkTargetIndex = -1
            onScrollToVerseConsumed()
        }
    }
    LaunchedEffect(playingIndex) {
        if (playingIndex >= 0 && playingIndex < verses.size) {
            // Always smoothly scroll the playing verse to near the top of the visible area
            listState.animateScrollToItem(
                index = playingIndex + 3,
                scrollOffset = 0
            )
        }
    }

    AdaptiveContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
        ) {
        // ── Top Bar: Back · Surah title pill ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(0.5.dp, Color(0x44FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Gold, RoundedCornerShape(16.dp))
                    .background(Color(0xFF233F40))
                    .padding(horizontal = 20.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Surah $surahNumber · $surahName",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Balancing spacer to keep pill visually centered
            Box(modifier = Modifier.size(44.dp))
        }


        // ── Reciter Selector Banner ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF0D3D3F), Color(0xFF143F3F)))
                )
                .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable { onChangeReciter() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic icon
            Icon(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECITER",
                    color = Gold.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = reciterName.ifBlank { "Mishary Rashid Alafasy" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            // Change arrow
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Change Reciter",
                    tint = Gold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Display Mode Tabs ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TabButton("Arabic", displayMode == DisplayMode.ARABIC) { onDisplayModeChange(DisplayMode.ARABIC) }
            Spacer(modifier = Modifier.width(6.dp))
            TabButton("Translation", displayMode == DisplayMode.TRANSLATION) { onDisplayModeChange(DisplayMode.TRANSLATION) }
            Spacer(modifier = Modifier.width(6.dp))
            TabButton("Both", displayMode == DisplayMode.BOTH) { onDisplayModeChange(DisplayMode.BOTH) }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 0: Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D504F))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF0D504F)), // Ideally surah_header_background
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(surahName, color = Gold, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(surahMeaning, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                    }
                }
            }

            // 1: About Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(14.dp)
                ) {
                    Text("ABOUT", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(getAboutText(surahNumber), color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }

            // 2: Font Controls & Counts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Verses: ${verses.size}", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Font size:", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = fontSize,
                        onValueChange = { onFontSizeChange(it) },
                        valueRange = 14f..40f,
                        modifier = Modifier.width(120.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700),
                            inactiveTrackColor = Color.White
                        )
                    )
                }
            }

            // Verses
            itemsIndexed(verses) { index, verse ->
                val isActive = playingIndex == index
                val isBookmarked = bookmarkedVerses.contains(verse.numberInSurah)
                val isBlink = blinkTargetIndex == index
                // Current reading position (topmost visible verse, only shown when not playing)
                val isReading = !isPlaying && readingVerseIndex == index

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .then(
                            if (isReading) Modifier.border(
                                width = 2.dp,
                                brush = Brush.verticalGradient(listOf(Gold, Color(0xFF1B7A7C))),
                                shape = RoundedCornerShape(16.dp)
                            ) else Modifier
                        )
                        .clickable { onVersePlay(index) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isBlink -> Color(0xFFFFD700).copy(alpha = blinkAlpha.value)
                            isActive && isPlaying -> Color(0xFFFFD700).copy(alpha = 0.15f)
                            isReading -> Color(0xFF1B7A7C).copy(alpha = 0.18f)
                            else -> Color.White.copy(alpha = 0.05f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                             Box(
                                modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(verse.numberInSurah.toString(), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                painter = painterResource(id = R.drawable.ic_verse_share),
                                contentDescription = "Share",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp).clickable { onVerseShare(verse) }.padding(4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = if (isActive && isPlaying) R.drawable.ic_verse_pause else R.drawable.ic_verse_play),
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp).clickable { onVersePlay(index) }.padding(4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_verse_bookmark),
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp).clickable { onVerseBookmark(verse.numberInSurah) }.padding(4.dp)
                            )
                        }

                        if (displayMode == DisplayMode.ARABIC || displayMode == DisplayMode.BOTH) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = verse.arabicText,
                                color = if (isActive && isPlaying) Color(0xFFFFD700) else Color.White,
                                fontSize = fontSize.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                fontWeight = if (quranScript == "Indopak") FontWeight.Bold else FontWeight.Normal,
                                lineHeight = (fontSize * 1.5f).sp
                            )
                        }

                        if (displayMode == DisplayMode.TRANSLATION || displayMode == DisplayMode.BOTH) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "${verse.numberInSurah}. ${verse.translation}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun RowScope.TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))) 
                else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.05f))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun getAboutText(number: Int): String = when (number) {
    1   -> "Al-Fatihah is a Makki surah revealed before Hijra. It is recited in every unit of prayer and is called the 'Mother of the Quran'. Whoever does not recite it in prayer, their prayer is incomplete."
    2   -> "Al-Baqarah is the longest surah in the Quran, revealed in Madinah. It covers Islamic law, stories of previous prophets, and the fundamental beliefs of Islam."
    18  -> "Al-Kahf is a Makki surah. Reciting it on Fridays brings light between two Fridays. It contains four major stories: the People of the Cave, the two gardens, Musa and Al-Khidr, and Dhul-Qarnayn."
    36  -> "Ya-Sin is the heart of the Quran. It emphasises resurrection and the power of Allah. It is recommended to recite it for the dying and for blessings."
    55  -> "Ar-Rahman is called the 'Beauty of the Quran'. The recurring verse 'Which of the favours of your Lord will you deny?' appears 31 times."
    67  -> "Al-Mulk is a Makki surah of 30 verses that intercedes for its reciter until they are forgiven. The Prophet ﷺ recited it every night before sleeping."
    112 -> "Al-Ikhlas is equal to one-third of the Quran in reward. It affirms the absolute oneness and uniqueness of Allah."
    113 -> "Al-Falaq is one of the Mu'awwidhatayn refuge surahs. It was revealed as protection against every evil, including the evil of the night, sorcery and envy."
    114 -> "An-Nas is the final surah of the Quran and the second of the two refuge surahs. It protects against the whispering of Shaytan into the hearts of people."
    else -> "This surah is part of the Noble Quran. Recite it with reflection and tranquility, seeking the mercy and guidance of Allah ﷻ."
}
