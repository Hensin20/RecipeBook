package com.example.recipebookkotlin.api

import com.example.recipebookkotlin.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/register")
    fun register(@Body user: User): Call<User>

    @POST("/auth/login")
    fun login(@Body user: User): Call<User>
}