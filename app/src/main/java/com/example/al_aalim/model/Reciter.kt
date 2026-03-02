package com.example.al_aalim.model

data class Reciter(
    val id: String,
    val nameEnglish: String,
    val country: String,
    val photoUrl: String = ""   // URL to load from internet; empty = show name only
)
