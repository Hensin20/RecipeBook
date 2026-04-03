package com.example.recipebookkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.navigation.fragment.findNavController

class FragmentHome : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Налаштовуємо список
        recyclerView = view.findViewById(R.id.recyclerView_recipes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Ініціалізуємо порожній адаптер
        adapter = RecipeAdapter(emptyList()) { recipeId ->
            val bundle = Bundle().apply {
                putLong("RECIPE_ID", recipeId)
            }

            // Звичайний перехід. Тепер системна кнопка "Назад" буде працювати автоматично!
            findNavController().navigate(R.id.fragmentViewRecipe, bundle)
        }
        recyclerView.adapter = adapter

        // Завантажуємо дані
        loadRecipes()
    }

    private fun loadRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recipes = ApiClient.recipeApi.getAllRecipes()

                withContext(Dispatchers.Main) {
                    adapter.updateData(recipes)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("API_ERROR", "Помилка: ${e.message}")
                    Toast.makeText(requireContext(), "Не вдалося завантажити рецепти", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}