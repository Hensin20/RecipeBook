package com.example.recipebookkotlin.dto

import java.util.Date

data class FavoriteDTO(
    val id: Long,
    val recipe: RecipeDTO,
    val addedAt: String? // Або Date, залежно від того, як ти парсиш дати
)