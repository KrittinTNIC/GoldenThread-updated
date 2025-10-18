package com.example.finalproject.data.model

data class User(
    val id: Int = 1,
    val firstName: String,
    val lastName: String,
    val email: String,
    val profileImage: String? = null
)