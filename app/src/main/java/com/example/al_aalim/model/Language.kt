package com.example.al_aalim.model

import androidx.annotation.DrawableRes

data class Language(
    val code: String,
    val nativeName: String,
    val englishName: String,
    @DrawableRes val flagResId: Int
)
