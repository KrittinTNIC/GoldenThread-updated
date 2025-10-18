package com.example.finalproject.model

data class Tour(
    val dramaId: String,
    val titleEn: String,
    val titleTh: String,
    val posterUrl: String,
    val locationCount: Int,
    val totalTravelTime: Int,
    val description: String
)