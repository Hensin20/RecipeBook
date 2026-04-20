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

        // Знаходимо елементи дизайну (перевір свої ID в XML!)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)

        btnLogin.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            // Перевірка на порожні поля
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Будь ласка, введіть Email та Пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Створюємо об'єкт для відправки
            val loginRequest = UserLoginDTO(email, password)

            // Відправляємо запит на сервер
            ApiClient.authApi.login(loginRequest).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    if (response.isSuccessful) {
                        val user = response.body()

                        // 1. Відкриваємо "сховище" SharedPreferences телефону
                        val sharedPreferences = getSharedPreferences("RecipeBookPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        // 2. Записуємо туди дані користувача
                        editor.putLong("USER_ID", user?.id ?: -1L)
                        editor.putString("USER_USERNAME", user?.username)
                        editor.putString("USER_EMAIL", user?.email)
                        editor.apply() // apply() зберігає це у фоновому режимі (швидко і безпечно)

                        Toast.makeText(this@ActivityLogin, "Вітаємо, ${user?.username}!", Toast.LENGTH_SHORT).show()

                        // 3. Перехід в Головне Меню (яке автоматично відкриє FragmentHome)
                        val intent = Intent(this@ActivityLogin, ActivityMenu::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Закриваємо екран логіну

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