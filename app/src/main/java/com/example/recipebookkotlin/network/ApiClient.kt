package com.example.recipebookkotlin.network
import com.example.recipebookkotlin.api.AuthApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
    private const val BASE_USER = "http://10.0.2.2:8080"

    val retrofit: Retrofit by lazy{
        Retrofit.Builder()
            .baseUrl(BASE_USER)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}