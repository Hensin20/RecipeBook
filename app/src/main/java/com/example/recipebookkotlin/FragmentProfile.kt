package com.example.recipebookkotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.Adapters.RecipeAdapter
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentProfile : Fragment() {

    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("USER_USERNAME", "Гість") ?: "Гість"
        val email = sharedPrefs.getString("USER_EMAIL", "")

        // Відображення даних
        view.findViewById<TextView>(R.id.profileUsername).text = username
        view.findViewById<TextView>(R.id.profileEmail).text = email

        // Налаштування списку
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMyRecipes)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RecipeAdapter(emptyList()) { recipeId ->
            // Перехід до перегляду рецепту
            val bundle = Bundle().apply { putLong("RECIPE_ID", recipeId) }
            // Тут додай навігацію (наприклад, через NavController)
        }
        recyclerView.adapter = adapter

        // Кнопка виходу
        view.findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            logoutUser()
        }

        loadMyRecipes(username)
    }

    private fun loadMyRecipes(username: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val myRecipes = ApiClient.recipeApi.getRecipesByAuthor(username)
                withContext(Dispatchers.Main) {
                    adapter.updateData(myRecipes)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не вдалося завантажити ваші рецепти", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun logoutUser() {
        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply() // ПОВНЕ ОЧИЩЕННЯ ДАНИХ

        // Перехід на початковий екран (MainActivity)
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}