package com.example.recipebookkotlin.network
import com.example.recipebookkotlin.api.AuthApi
import com.example.recipebookkotlin.api.CategoryApi
import com.example.recipebookkotlin.api.IngredientApi
import com.example.recipebookkotlin.api.RecipeApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {

    public const val ipAdres = "http://192.168.31.253:8081"
    private const val BASE_USER = ipAdres

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
    val recipeApi: RecipeApi by lazy {
        retrofit.create(RecipeApi::class.java)
    }
}