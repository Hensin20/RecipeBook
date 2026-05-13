package com.example.recipebookkotlin.network

import com.example.recipebookkotlin.api.AuthApi
import com.example.recipebookkotlin.api.CategoryApi
import com.example.recipebookkotlin.api.IngredientApi
import com.example.recipebookkotlin.api.RecipeApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Вже НЕ const! Тепер цю змінну можна міняти з коду
    var ipAdres = "http://192.168.31.252:8081/"

    private var retrofitInstance: Retrofit? = null

    // Функція, яку ми будемо викликати при зміні IP
    fun updateBaseUrl(newUrl: String) {
        ipAdres = newUrl
        retrofitInstance = null // Скидаємо старе з'єднання
    }

    // Динамічне створення Retrofit
    val retrofit: Retrofit
        get() {
            if (retrofitInstance == null) {
                retrofitInstance = Retrofit.Builder()
                    .baseUrl(ipAdres)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofitInstance!!
        }

    // ВАЖЛИВО: замість "by lazy" тепер використовуємо "get()"
    // Це гарантує, що при зміні IP всі API підхоплять нову адресу
    val authApi: AuthApi
        get() = retrofit.create(AuthApi::class.java)

    val ingredientApi: IngredientApi
        get() = retrofit.create(IngredientApi::class.java)

    val categoryApi: CategoryApi
        get() = retrofit.create(CategoryApi::class.java)

    val recipeApi: RecipeApi
        get() = retrofit.create(RecipeApi::class.java)
}