package com.example.recipebookkotlin.api

import com.example.recipebookkotlin.dto.ingredientsDictionaryDTO
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IngredientApi {
    @GET("/api/ingredients")
    suspend fun getIngredients(): List<ingredientsDictionaryDTO>
    @POST("/api/ingredients")
    suspend fun addIngredient(@Body ingredient: ingredientsDictionaryDTO): ingredientsDictionaryDTO
}