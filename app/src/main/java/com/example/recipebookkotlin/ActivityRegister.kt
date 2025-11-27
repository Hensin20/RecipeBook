package com.example.recipebookkotlin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recipebookkotlin.model.User
import com.example.recipebookkotlin.network.ApiClient
import retrofit2.Response
import retrofit2.Callback
import retrofit2.Call

class ActivityRegister : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val editUserName = findViewById<EditText>(R.id.editUsername)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val editPassword2 = findViewById<EditText>(R.id.editPassword2)
        val btnRegister = findViewById<Button>(R.id.buttonRegistration)

        btnRegister.setOnClickListener {
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()
            val password2 = editPassword2.text.toString()
            val username = editUserName.text.toString()

            val user = User(
                email = email,
                password = password,
                username = username,
                role = "user"
            )


            ApiClient.authApi.register(user).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ActivityRegister,
                            "Registered: ${response.body()?.email}",
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ActivityRegister,
                            "Error: ${response.code()}",
                            Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    Toast.makeText(this@ActivityRegister,
                        "Failure: ${t.message}",

                        Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}