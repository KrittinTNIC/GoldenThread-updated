package com.example.finalproject.model

data class LocationDetail(
    val id: String,
    val name: String,
    val address: String,
    val imageUrl: String,
    val description: String,
    val travelTimeFromPrevious: Int
)