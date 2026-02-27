package com.example.recipebookkotlin.api

import com.example.recipebookkotlin.dto.CategoryDTO
import retrofit2.http.GET

interface CategoryApi {
    @GET("/api/category")
    suspend fun getCategory(): List<CategoryDTO>
}