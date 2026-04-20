package com.example.recipebookkotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- КРОК 1: ПЕРЕВІРКА АВТОРИЗАЦІЇ ---
        val sharedPreferences = getSharedPreferences("RecipeBookPrefs", MODE_PRIVATE)
        val savedUserId = sharedPreferences.getLong("USER_ID", -1L)

        if (savedUserId != -1L) {
            // Користувач вже авторизований! Перекидаємо одразу в меню
            val intent = Intent(this, ActivityMenu::class.java)
            startActivity(intent)
            finish() // Закриваємо цей екран
            return   // Зупиняємо виконання коду тут, щоб не малювати кнопки нижче
        }
        // --------------------------------------

        // Якщо ми дійшли сюди, значить користувач НЕ авторизований.
        // Показуємо твій стартовий екран з кнопками!

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnRegister = findViewById<Button>(R.id.buttonRegistration)
        btnRegister.setOnClickListener {
            val intent = Intent(this, ActivityRegister::class.java)
            startActivity(intent)
        }

        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        btnLogin.setOnClickListener {
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
        }

        val btnImgTest = findViewById<View>(R.id.imageViewBackground7)
        btnImgTest.setOnClickListener {
            val intent = Intent(this, ActivityMenu::class.java)
            startActivity(intent)
        }
    }
}