package com.example.al_aalim

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.al_aalim.adapter.DisplayMode
import com.example.al_aalim.adapter.Verse
import com.example.al_aalim.adapter.VerseAdapter
import com.example.al_aalim.databinding.ActivitySurahReaderBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SurahReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurahReaderBinding
    private lateinit var verseAdapter: VerseAdapter
    private var verses: List<Verse> = emptyList()

    // Audio
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentVerseIndex = 0

    // Reciter SharedPrefs
    private val prefs by lazy { getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    // EveryAyah folder map — verified against https://everyayah.com/data/recitations.js
    private val reciterFolderMap = mapOf(
        "alafasy"    to "Alafasy_128kbps",
        "sudais"     to "Abdurrahmaan_As-Sudais_192kbps",
        "shatri"     to "Abu_Bakr_Ash-Shaatree_128kbps",
        "shuraym"    to "Saood_ash-Shuraym_128kbps",
        "husary"     to "Husary_128kbps",
        "muaiqly"    to "MaherAlMuaiqly128kbps",
        "ghamdi"     to "Ghamadi_40kbps",
        "abdulbasit" to "Abdul_Basit_Murattal_64kbps",
        "minshawi"   to "Minshawy_Murattal_128kbps",
        "hanirifai"  to "Hani_Rifai_192kbps",
        "ajmi"       to "ahmed_ibn_ali_al_ajamy_128kbps",
        "dosari"     to "Yasser_Ad-Dussary_128kbps"
    )

    // Surah info from intent
    private var surahNumber  = 1
    private var surahName    = "Al-Fatihah"
    private var surahMeaning = "The Opening"

    private val gson = Gson()

    companion object {
        const val EXTRA_SURAH_NUMBER  = "surah_number"
        const val EXTRA_SURAH_NAME    = "surah_name"
        const val EXTRA_SURAH_MEANING = "surah_meaning"
        const val REQUEST_RECITER     = 101
    }

    // ───────────────────────────── Lifecycle ─────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySurahReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        surahNumber  = intent.getIntExtra(EXTRA_SURAH_NUMBER, 1)
        surahName    = intent.getStringExtra(EXTRA_SURAH_NAME)    ?: "Al-Fatihah"
        surahMeaning = intent.getStringExtra(EXTRA_SURAH_MEANING) ?: "The Opening"

        setupHeader()
        setupRecyclerView()
        setupTabs()
        setupFontSlider()
        setupAudioControls()
        setupReciterButton()

        loadVerses()
    }

    override fun onPause() {
        super.onPause()
        pauseAudio()
    }

    override fun onResume() {
        super.onResume()
        // Update the script setting when coming back from Settings
        if (::verseAdapter.isInitialized) {
            val script = prefs.getString("quran_script", "Uthmani") ?: "Uthmani"
            verseAdapter.quranScript = script
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    // ─────────────────────────── UI Setup ────────────────────────────────

    private fun setupHeader() {
        binding.tvSurahLabel.text         = "SURAH $surahNumber"
        binding.tvSurahNameHeader.text    = surahName
        binding.tvSurahMeaningHeader.text = surahMeaning
        binding.tvAbout.text = aboutText(surahNumber)
        updateReciterLabel()
    }

    private fun setupRecyclerView() {
        verseAdapter = VerseAdapter(emptyList(), surahName) { numberInSurah ->
            playSingleVerse(numberInSurah - 1)
        }
        val currentScript = prefs.getString("quran_script", "Uthmani") ?: "Uthmani"
        verseAdapter.quranScript = currentScript
        binding.rvVerses.apply {
            layoutManager = LinearLayoutManager(this@SurahReaderActivity)
            adapter = verseAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupTabs() {
        listOf(binding.tabArabic, binding.tabTranslation, binding.tabBoth).forEach { tab ->
            tab.setOnClickListener { selectTab(it as TextView) }
        }
    }

    private fun selectTab(selected: TextView) {
        listOf(binding.tabArabic, binding.tabTranslation, binding.tabBoth).forEach { tab ->
            tab.setBackgroundResource(R.drawable.tab_chip_unselected)
            tab.setTextColor(getColor(android.R.color.white))
            tab.textSize = 13f
            tab.typeface = android.graphics.Typeface.DEFAULT
        }
        selected.setBackgroundResource(R.drawable.tab_chip_selected_gold)
        selected.setTextColor(getColor(android.R.color.white))
        selected.textSize = 13f
        selected.typeface = android.graphics.Typeface.DEFAULT_BOLD

        verseAdapter.displayMode = when (selected.id) {
            R.id.tab_arabic      -> DisplayMode.ARABIC
            R.id.tab_translation -> DisplayMode.TRANSLATION
            else                 -> DisplayMode.BOTH
        }
    }

    private fun setupFontSlider() {
        binding.seekbarFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                verseAdapter.arabicTextSizeSp = 14f + progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ─────────────────────────── Reciter ─────────────────────────────────

    private fun setupReciterButton() {
        // Tapping the reciter name area opens reciter selection
        binding.reciterNameArea.setOnClickListener {
            val intent = Intent(this, ReciterSelectionActivity::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_RECITER)
        }

        // Tapping the play icon toggles play/pause
        binding.ivReciterIcon.setOnClickListener {
            if (isPlaying) pauseAudio() else resumeOrPlay()
        }

        // Dark tint so icon is readable on gold background
        binding.ivReciterIcon.setColorFilter(android.graphics.Color.parseColor("#1A1A1A"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RECITER) {
            updateReciterLabel()
            // Stop current audio so next play uses the newly selected reciter
            stopAndResetAudio()
        }
    }

    private fun updateReciterLabel() {
        val name = prefs.getString("selected_reciter_name", "Mishary Alafasy") ?: "Mishary Alafasy"
        binding.tvReciterName.text = name.split(" ").lastOrNull() ?: name
    }

    // ─────────────────────────── Audio Controls ───────────────────────────

    private fun setupAudioControls() {
        binding.ivBack.setOnClickListener { finish() }
        binding.ivPrevVerse.setOnClickListener { prevVerse() }
        binding.ivNextVerse.setOnClickListener { nextVerse() }
    }

    private fun resumeOrPlay() {
        val player = mediaPlayer
        if (player != null && !isPlaying) {
            // Resume paused media
            player.start()
            isPlaying = true
            updatePlayPauseIcon()
        } else {
            // Start fresh from currentVerseIndex
            playVerseAtIndex(currentVerseIndex)
        }
    }

    fun playSingleVerse(index: Int) {
        if (index == currentVerseIndex && mediaPlayer != null) {
            if (isPlaying) pauseAudio() else resumeOrPlay()
        } else {
            currentVerseIndex = index
            playVerseAtIndex(index)
        }
    }

    private fun playVerseAtIndex(index: Int) {
        if (verses.isEmpty() || index < 0 || index >= verses.size) return

        val verse     = verses[index]
        val reciterId = prefs.getString("selected_reciter", "alafasy") ?: "alafasy"
        val folder    = reciterFolderMap[reciterId] ?: reciterFolderMap["alafasy"]!!
        val surahStr  = surahNumber.toString().padStart(3, '0')
        val ayahStr   = verse.numberInSurah.toString().padStart(3, '0')
        val url       = "https://everyayah.com/data/$folder/$surahStr$ayahStr.mp3"

        stopAndResetAudio()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(url)
                prepareAsync()

                setOnPreparedListener {
                    start()
                    this@SurahReaderActivity.isPlaying = true
                    updatePlayPauseIcon()
                    updateVerseHighlight(index)
                    scrollToVerse(index)
                    updateVerseLabel(verse.numberInSurah)
                }

                setOnCompletionListener {
                    // Auto-advance to next verse
                    val next = index + 1
                    if (next < verses.size) {
                        currentVerseIndex = next
                        playVerseAtIndex(currentVerseIndex)
                    } else {
                        // End of surah
                        this@SurahReaderActivity.isPlaying = false
                        updatePlayPauseIcon()
                        verseAdapter.playingIndex = -1
                    }
                }

                setOnErrorListener { _, _, _ ->
                    Toast.makeText(
                        this@SurahReaderActivity,
                        "Audio unavailable for this reciter",
                        Toast.LENGTH_SHORT
                    ).show()
                    this@SurahReaderActivity.isPlaying = false
                    updatePlayPauseIcon()
                    false
                }
            } catch (e: Exception) {
                Toast.makeText(this@SurahReaderActivity, "Unable to load audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseIcon()
    }

    private fun stopAndResetAudio() {
        mediaPlayer?.run {
            try { if (isPlaying) stop() } catch (_: Exception) {}
            reset()
            release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    private fun releasePlayer() {
        stopAndResetAudio()
    }

    private fun nextVerse() {
        if (currentVerseIndex < verses.size - 1) {
            currentVerseIndex++
            playVerseAtIndex(currentVerseIndex)
        } else {
            Toast.makeText(this, "Last verse", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prevVerse() {
        if (currentVerseIndex > 0) {
            currentVerseIndex--
            playVerseAtIndex(currentVerseIndex)
        } else {
            Toast.makeText(this, "First verse", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlayPauseIcon() {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.ivReciterIcon.setImageResource(iconRes)
        
        // Sync to adapter
        if (::verseAdapter.isInitialized) {
            verseAdapter.isMediaPlaying = isPlaying
        }
    }

    private fun updateVerseHighlight(index: Int) {
        verseAdapter.playingIndex = index
        verseAdapter.isMediaPlaying = isPlaying
    }

    private fun scrollToVerse(index: Int) {
        (binding.rvVerses.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(index, 120)
    }

    private fun updateVerseLabel(number: Int) {
        val total = verses.size
        binding.tvAyahCounter.text = if (total > 0) "Ayah $number / $total" else "Ayah $number"
    }

    // ─────────────────────────── Data Loading ────────────────────────────

    private data class SurahJson(val s: Int, val v: List<VerseJson>)
    private data class VerseJson(val n: Int, val a: String, val e: String)

    private fun loadVerses() {
        binding.loadingOverlay.visibility = android.view.View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json      = assets.open("quran.json").bufferedReader().use { it.readText() }
                val type      = object : TypeToken<List<SurahJson>>() {}.type
                val allSurahs = gson.fromJson<List<SurahJson>>(json, type)

                val surahData   = allSurahs.firstOrNull { it.s == surahNumber }
                val builtVerses = surahData?.v?.map { v ->
                    Verse(numberInSurah = v.n, arabicText = v.a, translation = v.e)
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    verses = builtVerses
                    verseAdapter = VerseAdapter(verses, surahName) { numberInSurah ->
                        playSingleVerse(numberInSurah - 1)
                    }
                    val currentScript = prefs.getString("quran_script", "Uthmani") ?: "Uthmani"
                    verseAdapter.quranScript = currentScript
                    verseAdapter.arabicTextSizeSp = 14f + binding.seekbarFont.progress
                    binding.rvVerses.adapter = verseAdapter
                    binding.tvVerseCount.text = "Verses: ${verses.size}"
                    binding.loadingOverlay.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = android.view.View.GONE
                    Toast.makeText(
                        this@SurahReaderActivity,
                        "Failed to load verses: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ─────────────────────────── About text ──────────────────────────────

    private fun aboutText(number: Int): String = when (number) {
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
}
