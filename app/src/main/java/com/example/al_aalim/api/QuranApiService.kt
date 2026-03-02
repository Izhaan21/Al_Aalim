package com.example.al_aalim.api

import com.example.al_aalim.model.SurahApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApiService {
    /**
     * Fetches a surah in two editions:
     *  - ar.alafasy  : Arabic text
     *  - en.sahih    : Sahih International English translation
     * Returns a list of two SurahEdition objects.
     */
    @GET("surah/{number}/editions/ar.alafasy,en.sahih")
    suspend fun getSurah(@Path("number") number: Int): SurahApiResponse
}
