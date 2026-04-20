package com.example.recipebookkotlin.api

import com.example.recipebookkotlin.model.User
import com.example.recipebookkotlin.model.UserLoginDTO
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/register")
    fun register(@Body user: User): Call<User>

    @POST("/api/auth/login")
    fun login(@Body request: UserLoginDTO): Call<User>
}