package com.example.recipebookkotlin.network

import com.example.recipebookkotlin.api.AuthApi
import com.example.recipebookkotlin.api.CategoryApi
import com.example.recipebookkotlin.api.IngredientApi
import com.example.recipebookkotlin.api.RecipeApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Повернули твою назву змінної!
    var ipAdres = "http://192.168.31.252:8081/"

    // Приватна змінна для зберігання налаштованого Retrofit
    private var retrofitInstance: Retrofit? = null

    // Розумна функція, яка віддає існуючий Retrofit або створює новий
    private fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(ipAdres) // Використовуємо ipAdres
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    // Функція для оновлення IP
    fun updateBaseUrl(newIp: String) {
        var formattedUrl = newIp
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (!formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }

        // Якщо IP дійсно новий, оновлюємо адресу і скидаємо старий Retrofit
        if (ipAdres != formattedUrl) {
            ipAdres = formattedUrl
            retrofitInstance = null // При наступному запиті getRetrofit() створить новий клієнт
        }
    }

    // Всі твої API
    val authApi: AuthApi
        get() = getRetrofit().create(AuthApi::class.java)

    val ingredientApi: IngredientApi
        get() = getRetrofit().create(IngredientApi::class.java)

    val categoryApi: CategoryApi
        get() = getRetrofit().create(CategoryApi::class.java)

    val recipeApi: RecipeApi
        get() = getRetrofit().create(RecipeApi::class.java)
}