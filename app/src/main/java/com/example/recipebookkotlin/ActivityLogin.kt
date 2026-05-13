package com.example.recipebookkotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog // Додано для віконця
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

        // 1. ЗАВАНТАЖЕННЯ ЗБЕРЕЖЕНОГО IP ПРИ СТАРТІ ДОДАТКА
        val sharedPreferences = getSharedPreferences("RecipeBookPrefs", MODE_PRIVATE)
        val savedIp = sharedPreferences.getString("SERVER_IP", "http://10.131.139.162:8080/") ?: "http://10.131.139.162:8080/"
        ApiClient.updateBaseUrl(savedIp) // Передаємо адресу в Retrofit

        // 2. ЗНАХОДИМО НАШ "СЕКРЕТНИЙ" БАКЛАЖАН
        val imgEggplant = findViewById<ImageView>(R.id.imageViewBackground8)
        imgEggplant.setOnClickListener {
            showIpConfigDialog()
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
                    Toast.makeText(this@ActivityLogin, "Помилка мережі: Перевірте IP сервера!", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    // 3. ФУНКЦІЯ ДЛЯ ВІДОБРАЖЕННЯ ВІКОНЦЯ НАЛАШТУВАНЬ
    private fun showIpConfigDialog() {
        val sharedPreferences = getSharedPreferences("RecipeBookPrefs", MODE_PRIVATE)
        val currentIp = sharedPreferences.getString("SERVER_IP", "http://10.131.139.162:8080/")

        // Створюємо поле для вводу тексту
        val editText = EditText(this).apply {
            setText(currentIp)
            setPadding(50, 50, 50, 50)
            hint = "http://192.168.X.X:8080/"
        }

        // Створюємо спливаюче вікно
        AlertDialog.Builder(this)
            .setTitle("Налаштування сервера ⚙️")
            .setMessage("Введіть нову IP-адресу сервера (не забудьте порт і слеш в кінці):")
            .setView(editText)
            .setPositiveButton("Зберегти") { _, _ ->
                var newIp = editText.text.toString().trim()

                if (newIp.isNotEmpty()) {
                    // Автоматично додаємо слеш в кінці, якщо користувач забув
                    if (!newIp.endsWith("/")) {
                        newIp += "/"
                    }
                    // Автоматично додаємо http://, якщо користувач забув
                    if (!newIp.startsWith("http")) {
                        newIp = "http://$newIp"
                    }

                    // Зберігаємо нову адресу в пам'ять телефону
                    sharedPreferences.edit().putString("SERVER_IP", newIp).apply()

                    // Відразу оновлюємо ApiClient
                    ApiClient.updateBaseUrl(newIp)

                    Toast.makeText(this, "IP змінено на: $newIp", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }
}