package com.example.al_aalim.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuranViewModelTest {

    private lateinit var viewModel: QuranViewModel

    @Before
    fun setUp() {
        viewModel = QuranViewModel()
    }

    @Test
    fun testInitialState() {
        val initialSurahs = viewModel.displayedSurahs.value
        assertEquals(114, initialSurahs.size)
        assertTrue(viewModel.favourites.value.isEmpty())
        assertFalse(viewModel.isFavouritesTabActive.value)
    }

    @Test
    fun testSearchFiltering() {
        // Search for "Ya-Sin" -> should find Surah Ya-Sin
        viewModel.setSearchQuery("Ya-Sin")
        val filtered = viewModel.displayedSurahs.value
        assertTrue(filtered.any { it.name.contains("Yasin", ignoreCase = true) || it.name.contains("Ya-Sin", ignoreCase = true) })
        
        // Search by Surah number "114" -> should only find An-Nas
        viewModel.setSearchQuery("114")
        val searchByNumber = viewModel.displayedSurahs.value
        assertEquals(1, searchByNumber.size)
        assertEquals(114, searchByNumber[0].number)
    }

    @Test
    fun testFavouriteToggle() {
        // Toggle Surah 1 as favourite
        viewModel.toggleFavourite(1)
        assertTrue(viewModel.favourites.value.contains(1))
        
        // Toggle again to remove
        viewModel.toggleFavourite(1)
        assertFalse(viewModel.favourites.value.contains(1))
    }

    @Test
    fun testFavouritesTabFiltering() {
        // Add two favourites
        viewModel.toggleFavourite(1)
        viewModel.toggleFavourite(2)
        
        // Switch to Favourites tab
        viewModel.setTab(isFavourites = true)
        assertTrue(viewModel.isFavouritesTabActive.value)
        
        val filtered = viewModel.displayedSurahs.value
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.number == 1 })
        assertTrue(filtered.any { it.number == 2 })
    }
}
