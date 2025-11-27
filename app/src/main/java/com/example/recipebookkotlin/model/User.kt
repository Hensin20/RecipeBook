package com.example.recipebookkotlin.model

data class User (
    val id: Long? = null,
    val email: String,
    val password: String,
    val username: String,
    val role: String
)
