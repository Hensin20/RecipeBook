package com.example.recipebookkotlin.dto

data class RecipeResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val categoryName: String,
    val authorName: String,
    val instruction: String?, // Бо може бути null
    val ingredients: List<IngredientDTO>,
    val imageUrls: List<String>? // Список назв файлів картинок
)