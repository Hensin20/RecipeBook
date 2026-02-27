package com.example.recipebookkotlin.network
import com.example.recipebookkotlin.api.AuthApi
import com.example.recipebookkotlin.api.CategoryApi
import com.example.recipebookkotlin.api.IngredientApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
    private const val BASE_USER = "http://192.168.0.101:8080"

    val retrofit: Retrofit by lazy{
        Retrofit.Builder()
            .baseUrl(BASE_USER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val ingredientApi: IngredientApi by lazy {
        retrofit.create(IngredientApi::class.java)
    }

    val categoryApi: CategoryApi by lazy {
        retrofit.create(CategoryApi::class.java)
    }
}