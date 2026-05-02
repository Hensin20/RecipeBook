package com.example.recipebookkotlin

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.Adapters.RecipeAdapter
import com.example.recipebookkotlin.dto.RecipeDTO
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentCatalog : Fragment() {
    private lateinit var recipeAdapter: RecipeAdapter
    private var allRecipes = listOf<RecipeDTO>()

    // Змінна для контролю пошукових запитів (захист від спаму)
    private var searchJob: Job? = null

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
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val results = ApiClient.recipeApi.searchByCategory(selectedCategory)
                    withContext(Dispatchers.Main) {
                        recipeAdapter.updateData(results)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Помилка завантаження категорії", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        recyclerCategories.adapter = categoryAdapter

        // Завантажуємо категорії
        val myCategories = listOf("Сніданки", "Обіди", "Вечері", "Десерти", "Випічка", "Напої", "Салати")
        categoryAdapter.updateData(myCategories)
        // -------------------------

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerViewCatalog)

        recipeAdapter = RecipeAdapter(
            recipes = emptyList(),
            onRecipeClick = { id ->
                val bundle = Bundle().apply { putLong("RECIPE_ID", id) }
                findNavController().navigate(R.id.fragmentViewRecipe, bundle)
            }
        )

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

    // --- ОНОВЛЕНИЙ РОЗУМНИЙ ПОШУК ЗАТРИМКОЮ (DEBOUNCE) ---
    private fun performSearch(query: String?) {
        // Одразу скасовуємо попередній запит, якщо користувач продовжує друкувати
        searchJob?.cancel()

        if (query.isNullOrBlank()) {
            recipeAdapter.updateData(allRecipes)
            return
        }

        // Створюємо новий запит
        searchJob = lifecycleScope.launch(Dispatchers.IO) {
            // Чекаємо 500 мілісекунд. Якщо за цей час користувач введе нову літеру,
            // цей Job буде скасовано (рядок searchJob?.cancel() вище)
            delay(500)

            try {
                val results = if (query.contains(",")) {
                    // Якщо в запиті є КОМА — шукаємо по комбінації інгредієнтів
                    ApiClient.recipeApi.searchByIngredients(query)
                } else {
                    // Якщо коми немає — звичайний пошук по назві рецепту
                    ApiClient.recipeApi.searchRecipes(query)
                }

                withContext(Dispatchers.Main) {
                    recipeAdapter.updateData(results)
                    // Підказуємо користувачеві, якщо нічого не знайшли
                    if (results.isEmpty() && query.contains(",")) {
                        Toast.makeText(requireContext(), "З такими інгредієнтами рецептів немає 😢", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Виводимо помилку ТІЛЬКИ якщо це реальна помилка сервера/мережі,
                // а не "штучна" помилка через те, що ми самі скасували запит (CancellationException)
                if (e !is kotlinx.coroutines.CancellationException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Помилка під час пошуку", Toast.LENGTH_SHORT).show()
                    }
                }
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Не вдалося завантажити рецепти", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}