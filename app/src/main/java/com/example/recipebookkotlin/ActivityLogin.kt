package com.example.recipebookkotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recipebookkotlin.model.User
import com.example.recipebookkotlin.model.UserLoginDTO
import com.example.recipebookkotlin.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivityLogin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)

        btnLogin.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Будь ласка, введіть Email та Пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = UserLoginDTO(email, password)

            ApiClient.authApi.login(loginRequest).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    if (response.isSuccessful) {
                        val user = response.body()

                        val sharedPreferences = getSharedPreferences("RecipeBookPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        editor.putLong("USER_ID", user?.id ?: -1L)
                        editor.putString("USER_USERNAME", user?.username)
                        editor.putString("USER_EMAIL", user?.email)

                        val isUserAdmin = (user?.role?.lowercase() == "admin") || (user?.isAdmin == true)
                        editor.putBoolean("IS_ADMIN", isUserAdmin)

                        editor.apply()

                        Toast.makeText(this@ActivityLogin, "Вітаємо, ${user?.username}!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@ActivityLogin, ActivityMenu::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this@ActivityLogin, "Невірний email або пароль!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    Toast.makeText(this@ActivityLogin, "Помилка мережі: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}