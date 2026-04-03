package com.example.recipebookkotlin.api

import com.example.recipebookkotlin.dto.RecipeDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface RecipeApi {

    // 1. Створення рецепту (POST)
    @Multipart
    @POST("/api/recipes")
    suspend fun createRecipe(
        @Part("recipe") recipeData: RequestBody,
        @Part images: List<MultipartBody.Part>?
    )

    // 2. Отримання ВСІХ рецептів для головної сторінки (GET)
    @GET("/api/recipes")
    suspend fun getAllRecipes(): List<RecipeDTO>

    // 3. Отримання ОДНОГО рецепту для перегляду (GET)
    @GET("/api/recipes/{id}")
    suspend fun getRecipeById(@Path("id") id: Long): RecipeDTO
}