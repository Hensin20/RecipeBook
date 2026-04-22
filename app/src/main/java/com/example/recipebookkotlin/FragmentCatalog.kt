package com.example.recipebookkotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// ВИПРАВЛЕНО: Правильний імпорт SearchView для AndroidX
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.Adapters.RecipeAdapter
import com.example.recipebookkotlin.dto.RecipeDTO
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentCatalog : Fragment() {
    private lateinit var recipeAdapter: RecipeAdapter
    private var allRecipes = listOf<RecipeDTO>()

    // ДОДАНО: Цього методу не вистачало для відображення дизайну!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- ДОДАЄМО КАТЕГОРІЇ ---
        val recyclerCategories = view.findViewById<RecyclerView>(R.id.recyclerViewCategories)

        // Робимо список ГОРИЗОНТАЛЬНИМ
        recyclerCategories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val categoryAdapter = com.example.recipebookkotlin.Adapters.CategoryAdapter(emptyList()) { selectedCategory ->
            // Що робити, коли натиснули на категорію?
            // Відправляємо запит на пошук за категорією!
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Звертаємося до ендпоінту, який ти вже створив на бекенді
                    val results = ApiClient.recipeApi.searchByCategory(selectedCategory)
                    withContext(Dispatchers.Main) {
                        recipeAdapter.updateData(results)
                    }
                } catch (e: Exception) {
                    // Обробка помилки
                }
            }
        }
        recyclerCategories.adapter = categoryAdapter

        // Завантажуємо категорії з бази (або поки що створюємо вручну для тесту)
        val myCategories = listOf("Сніданки", "Обіди", "Вечері", "Десерти", "Випічка", "Напої", "Салати")
        categoryAdapter.updateData(myCategories)
        // -------------------------

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerViewCatalog)

        recipeAdapter = RecipeAdapter(emptyList()) { id ->
            val bundle = Bundle().apply { putLong("RECIPE_ID", id) }
            findNavController().navigate(R.id.fragmentViewRecipe, bundle)
        }
        recycler.adapter = recipeAdapter
        recycler.layoutManager = LinearLayoutManager(context)

        // Логіка пошуку
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }
        })

        loadInitialData()
    }

    private fun performSearch(query: String?) {
        if (query.isNullOrBlank()) {
            recipeAdapter.updateData(allRecipes)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val results = ApiClient.recipeApi.searchRecipes(query)
                withContext(Dispatchers.Main) {
                    recipeAdapter.updateData(results)
                }
            } catch (e: Exception) {
                // обробка помилок
            }
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recipes = ApiClient.recipeApi.getAllRecipes()
                allRecipes = recipes
                withContext(Dispatchers.Main) {
                    recipeAdapter.updateData(recipes)
                }
            } catch (e: Exception) {
                // обробка помилок
            }
        }
    }
}