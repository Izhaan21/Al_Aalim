package com.example.al_aalim.model

data class SurahApiResponse(
    val code: Int,
    val status: String,
    val data: List<SurahEdition>
)

data class SurahEdition(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val edition: EditionInfo,
    val ayahs: List<AyahData>
)

data class EditionInfo(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val format: String,
    val type: String,
    val direction: String?
)

data class AyahData(
    val number: Int,          // global ayah number
    val text: String,         // the text (Arabic or translation)
    val numberInSurah: Int,
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    val sajda: Boolean
)
