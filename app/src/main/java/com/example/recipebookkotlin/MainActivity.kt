package com.example.recipebookkotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recipebookkotlin.network.ApiClient

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. Відкриваємо ТЕ САМЕ сховище налаштувань, що й у ActivityLogin ("RecipeBookPrefs")
        val sharedPreferences = getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)

        // 2. Дістаємо збережений IP (поставив твій IP з ApiClient як дефолтний)
        val savedIp = sharedPreferences.getString("SERVER_IP", "http://192.168.31.252:8081/")

        // 3. Передаємо збережений IP у наш ApiClient
        if (savedIp != null) {
            ApiClient.updateBaseUrl(savedIp)
        }

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