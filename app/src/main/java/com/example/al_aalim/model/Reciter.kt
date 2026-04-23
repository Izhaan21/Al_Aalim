package com.example.al_aalim.model

data class Reciter(
    val id: String,
    val nameEnglish: String,
    val style: String,          // e.g. "Murattal"
    val quality: String = "",    // e.g. "High Quality"
    val photoUrl: String = ""
)
