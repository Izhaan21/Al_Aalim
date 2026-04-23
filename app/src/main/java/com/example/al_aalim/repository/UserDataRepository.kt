package com.example.al_aalim.repository

import android.content.Context
import com.example.al_aalim.model.BookmarkedVerse
import com.example.al_aalim.model.ReadingHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserDataRepository(private val context: Context) {

    private val gson = Gson()
    private val prefsName = "UserData"
    private val maxHistorySize = 50

    private fun prefs() = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    // ─────────────────────────── Verse Bookmarks ─────────────────────────

    private fun bookmarkKey(userId: String) = "${userId}_bookmarks"

    fun getBookmarks(userId: String): List<BookmarkedVerse> {
        val json = prefs().getString(bookmarkKey(userId), null) ?: return emptyList()
        val type = object : TypeToken<List<BookmarkedVerse>>() {}.type
        return try { gson.fromJson(json, type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun addBookmark(
        userId: String,
        surahNumber: Int, surahName: String, surahMeaning: String,
        verseNumber: Int, arabicText: String, translation: String
    ) {
        val list = getBookmarks(userId).toMutableList()
        if (list.none { it.surahNumber == surahNumber && it.verseNumber == verseNumber }) {
            list.add(0, BookmarkedVerse(surahNumber, surahName, surahMeaning, verseNumber, arabicText, translation))
            prefs().edit().putString(bookmarkKey(userId), gson.toJson(list)).apply()
        }
    }

    fun removeBookmark(userId: String, surahNumber: Int, verseNumber: Int) {
        val list = getBookmarks(userId).filter {
            !(it.surahNumber == surahNumber && it.verseNumber == verseNumber)
        }
        prefs().edit().putString(bookmarkKey(userId), gson.toJson(list)).apply()
    }

    fun toggleBookmark(
        userId: String,
        surahNumber: Int, surahName: String, surahMeaning: String,
        verseNumber: Int, arabicText: String, translation: String
    ): Boolean {
        return if (isBookmarked(userId, surahNumber, verseNumber)) {
            removeBookmark(userId, surahNumber, verseNumber)
            false
        } else {
            addBookmark(userId, surahNumber, surahName, surahMeaning, verseNumber, arabicText, translation)
            true
        }
    }

    fun isBookmarked(userId: String, surahNumber: Int, verseNumber: Int): Boolean =
        getBookmarks(userId).any { it.surahNumber == surahNumber && it.verseNumber == verseNumber }

    fun clearBookmarks(userId: String) {
        prefs().edit().remove(bookmarkKey(userId)).apply()
    }

    fun removeBookmarkAt(userId: String, surahNumber: Int, verseNumber: Int, addedAt: Long) {
        val list = getBookmarks(userId).filter {
            !(it.surahNumber == surahNumber && it.verseNumber == verseNumber && it.addedAt == addedAt)
        }
        prefs().edit().putString(bookmarkKey(userId), gson.toJson(list)).apply()
    }

    // ─────────────────────────── Reading History ──────────────────────────

    private fun histKey(userId: String) = "${userId}_reading_history"

    fun getReadingHistory(userId: String): List<ReadingHistoryItem> {
        val json = prefs().getString(histKey(userId), null) ?: return emptyList()
        val type = object : TypeToken<List<ReadingHistoryItem>>() {}.type
        return try { gson.fromJson(json, type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    /**
     * Upsert: if surah already in history, update lastVerseNumber + readAt and move to top.
     * If new, insert at head. Caps at maxHistorySize.
     */
    fun addReadingHistory(
        userId: String,
        surahNumber: Int, surahName: String, surahMeaning: String,
        lastVerseNumber: Int = 1
    ) {
        val list = getReadingHistory(userId).toMutableList()
        list.removeAll { it.surahNumber == surahNumber }
        list.add(0, ReadingHistoryItem(surahNumber, surahName, surahMeaning, lastVerseNumber))
        val capped = if (list.size > maxHistorySize) list.subList(0, maxHistorySize) else list
        prefs().edit().putString(histKey(userId), gson.toJson(capped)).apply()
    }

    fun removeHistoryItem(userId: String, surahNumber: Int, readAt: Long) {
        val list = getReadingHistory(userId).filter {
            !(it.surahNumber == surahNumber && it.readAt == readAt)
        }
        prefs().edit().putString(histKey(userId), gson.toJson(list)).apply()
    }

    fun clearReadingHistory(userId: String) {
        prefs().edit().remove(histKey(userId)).apply()
    }
}
