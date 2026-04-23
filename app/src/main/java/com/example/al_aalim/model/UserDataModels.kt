package com.example.al_aalim.model

/**
 * A bookmarked verse — stores surah context and the specific verse.
 */
data class BookmarkedVerse(
    val surahNumber: Int,
    val surahName: String,
    val surahMeaning: String,
    val verseNumber: Int,
    val arabicText: String,
    val translation: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Reading history entry — tracks the last verse the user read in a surah.
 */
data class ReadingHistoryItem(
    val surahNumber: Int,
    val surahName: String,
    val surahMeaning: String,
    val lastVerseNumber: Int = 1,
    val readAt: Long = System.currentTimeMillis()
)
