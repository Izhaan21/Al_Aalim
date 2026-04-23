package com.example.al_aalim.viewmodel

import androidx.lifecycle.ViewModel
import com.example.al_aalim.model.Surah
import com.example.al_aalim.model.SurahData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuranViewModel : ViewModel() {

    private val allSurahs = SurahData.getAllSurahs()

    // Holds the set of favourite surah numbers.
    private val _favourites = MutableStateFlow<Set<Int>>(emptySet())
    val favourites: StateFlow<Set<Int>> = _favourites.asStateFlow()

    // Holds the currently active tab (true if Favourites, false if All)
    private val _isFavouritesTabActive = MutableStateFlow(false)
    val isFavouritesTabActive: StateFlow<Boolean> = _isFavouritesTabActive.asStateFlow()

    // Holds the current search query
    private val _searchQuery = MutableStateFlow("")

    // The final list to display
    private val _displayedSurahs = MutableStateFlow<List<Surah>>(allSurahs)
    val displayedSurahs: StateFlow<List<Surah>> = _displayedSurahs.asStateFlow()

    fun toggleFavourite(surahNumber: Int) {
        _favourites.update { currentFavs ->
            val newFavs = currentFavs.toMutableSet()
            if (newFavs.contains(surahNumber)) {
                newFavs.remove(surahNumber)
            } else {
                newFavs.add(surahNumber)
            }
            newFavs
        }
        applyFilters()
    }
    
    fun setFavourites(favs: Set<Int>) {
        _favourites.value = favs
        applyFilters()
    }

    fun setTab(isFavourites: Boolean) {
        _isFavouritesTabActive.value = isFavourites
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        val favs = _favourites.value
        val showFavsOnly = _isFavouritesTabActive.value
        val query = _searchQuery.value.trim()

        val baseList = if (showFavsOnly) {
            allSurahs.filter { favs.contains(it.number) }
        } else {
            allSurahs
        }

        val filteredList = if (query.isEmpty()) {
            baseList
        } else {
            baseList.filter { s ->
                s.number.toString() == query ||
                        s.name.contains(query, ignoreCase = true) ||
                        s.meaning.contains(query, ignoreCase = true) ||
                        s.arabicName.contains(query, ignoreCase = true)
            }
        }

        _displayedSurahs.value = filteredList
    }
}
