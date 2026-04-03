package com.example.recipebookkotlin.dto

// Головний об'єкт, який ми відправляємо на сервер
data class RecipeCreateDTO(
    val title: String,
    val description: String,
    val categoryName: String, // Тепер тут Name замість Id
    val authorName: String,   // Додали ім'я автора
    val instruction: String,
    val ingredients: List<IngredientDTO> // Використовуємо нову назву класу
)

// Об'єкт для кожного окремого інгредієнта
data class IngredientDTO(
    val name: String,
    val quantity: String
)