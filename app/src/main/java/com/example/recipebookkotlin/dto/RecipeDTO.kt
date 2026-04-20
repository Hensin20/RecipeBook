package com.example.recipebookkotlin.dto

data class RecipeDTO(
    val id: Long,
    val title: String,
    val description: String?,       // Може бути null, якщо опис порожній
    val averageRating: Double,
    val votesCount: Int,
    val categoryName: String,
    val authorName: String,
    val instruction: String?,       // Кроки приготування
    val imageUrls: List<String>?,   // Список назв фотографій
    val ingredients: List<IngredientDTO>? // Список інгредієнтів
)