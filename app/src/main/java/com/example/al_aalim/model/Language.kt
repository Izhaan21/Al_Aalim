package com.example.al_aalim.model

data class Language(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flagEmoji: String  // Changed from @DrawableRes flagResId: Int
)
