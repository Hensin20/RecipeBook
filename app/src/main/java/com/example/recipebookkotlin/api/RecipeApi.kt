package com.example.recipebookkotlin.api

import com.example.recipebookkotlin.dto.CategoryDTO
import com.example.recipebookkotlin.dto.FavoriteDTO
import com.example.recipebookkotlin.dto.RecipeDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    // 4. Відправка оцінки за рецепт
    @POST("/api/recipes/{id}/rate")
    suspend fun rateRecipe(
        @Path("id") id: Long,
        @Query("rating") rating: Int
    ): Double // Сервер поверне нам нове середнє значення

    @GET("/api/recipes/author/{username}")
    suspend fun getRecipesByAuthor(@Path("username") username: String): List<RecipeDTO>

    @GET("/api/favorites/{username}")
    suspend fun getFavorites(@Path("username") username: String): List<FavoriteDTO>

    // Додати в закладки
    @POST("/api/favorites/add")
    suspend fun addToFavorites(
        @Query("username") username: String,
        @Query("recipeId") recipeId: Long
    ): Response<Unit>

    // Видалити з закладок
    @DELETE("/api/favorites/remove")
    suspend fun removeFromFavorites(
        @Query("username") username: String,
        @Query("recipeId") recipeId: Long
    ): Response<Unit>

    // Отримати список усіх категорій
    @GET("/api/categories")
    suspend fun getAllCategories(): List<CategoryDTO>

    // Пошук за назвою або інгредієнтом
    @GET("/api/recipes/search")
    suspend fun searchRecipes(
        @Query("query") query: String
    ): List<RecipeDTO>

    // Пошук рецептів за назвою категорії
    @GET("/api/recipes/search-by-category")
    suspend fun searchByCategory(
        @Query("category") category: String
    ): List<RecipeDTO>

}