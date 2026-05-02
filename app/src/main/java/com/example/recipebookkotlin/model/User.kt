package com.example.recipebookkotlin.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long = 0, // Може бути без "= 0", якщо в тебе було інакше
    val username: String,
    val email: String,

    // ПОВЕРТАЄМО ПАРОЛЬ:
    val password: String,

    val role: String? = "user",
    @SerializedName("admin", alternate = ["isAdmin"])
    val isAdmin: Boolean = false
)