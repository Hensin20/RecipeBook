package com.example.recipebookkotlin.dto

data class FavoriteDTO(
    val id: Long,
    val addedAt: String?,

    // ДОДАЙ ОСЬ ЦЕЙ РЯДОК:
    val collectionName: String?,

    val recipe: RecipeDTO
)