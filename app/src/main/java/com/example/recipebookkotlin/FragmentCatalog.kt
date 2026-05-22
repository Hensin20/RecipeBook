package com.example.recipebookkotlin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.PopupMenu
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

    private var searchJob: Job? = null

    // Змінна для відстеження поточного типу пошуку
    private var isSearchByIngredient = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- КАТЕГОРІЇ ---
        val recyclerCategories = view.findViewById<RecyclerView>(R.id.recyclerViewCategories)
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
        val myCategories = listOf("Сніданки", "Обіди", "Вечері", "Десерти", "Випічка", "Напої", "Салати")
        categoryAdapter.updateData(myCategories)
        // -------------------------

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val searchAutoComplete = searchView.findViewById<AutoCompleteTextView>(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete.setTextColor(android.graphics.Color.BLACK)
        searchAutoComplete.setHintTextColor(android.graphics.Color.GRAY)

        val btnFilter = view.findViewById<ImageButton>(R.id.btnSearchFilter)
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

        // Налаштування PopupMenu для кнопки фільтра
        btnFilter.setOnClickListener { filterView ->
            val popup = PopupMenu(requireContext(), filterView)

            // Додаємо галочку до поточного обраного пункту
            val nameTitle = if (!isSearchByIngredient) "✓ За назвою" else "За назвою"
            val ingredientTitle = if (isSearchByIngredient) "✓ За інгредієнтом" else "За інгредієнтом"

            popup.menu.add(0, 1, 0, nameTitle)
            popup.menu.add(0, 2, 0, ingredientTitle)

            // ... (всередині btnFilter.setOnClickListener)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        isSearchByIngredient = false
                        searchView.queryHint = "Введіть назву..."
                        performSearch(searchView.query.toString())
                    }
                    2 -> {
                        isSearchByIngredient = true
                        // Змінюємо підказку!
                        searchView.queryHint = "Напр: сир, помідор, яйця..."
                        performSearch(searchView.query.toString())
                    }
                }
                true
            }
            popup.show()
        }

        // Логіка пошуку при введенні тексту
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Користувач натиснув "Пошук" (Enter на клавіатурі)
                searchView.clearFocus() // Ховаємо клавіатуру
                performSearch(query)    // Запускаємо пошук
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Якщо користувач повністю стер текст (натиснув хрестик),
                // повертаємо весь список рецептів
                if (newText.isNullOrBlank()) {
                    recipeAdapter.updateData(allRecipes)
                }
                // Тут ми більше не викликаємо performSearch(newText)!
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
                // Вибираємо запит до API залежно від обраного фільтра
                val results = if (isSearchByIngredient) {
                    val cleanQuery = query.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(",")

                    ApiClient.recipeApi.searchByIngredients(cleanQuery)
                } else {
                    ApiClient.recipeApi.searchRecipes(query)
                }

                withContext(Dispatchers.Main) {
                    recipeAdapter.updateData(results)

                    if (results.isEmpty()) {
                        val message = if (isSearchByIngredient) {
                            "Рецептів з такими інгредієнтами не знайдено 😢"
                        } else {
                            "Рецептів з такою назвою не знайдено 😢"
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка під час пошуку", Toast.LENGTH_SHORT).show()
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
                    setupAutoComplete(recipes)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Не вдалося завантажити рецепти", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupAutoComplete(recipes: List<RecipeDTO>) {
        val recipeNames = recipes.map { it.title }
        val ingredientNames = recipes.flatMap { it.ingredients?.map { ing -> ing.name } ?: emptyList() }
        val allSuggestions = (recipeNames + ingredientNames).distinct()

        val searchView = view?.findViewById<SearchView>(R.id.searchView)
        val searchAutoComplete = searchView?.findViewById<AutoCompleteTextView>(androidx.appcompat.R.id.search_src_text)

        if (searchAutoComplete != null) {
            searchAutoComplete.setDropDownBackgroundResource(android.R.color.white)

            val adapter = ArrayAdapter<String>(
                requireContext(),
                R.layout.item_search_suggestion,
                allSuggestions
            )
            searchAutoComplete.setAdapter(adapter)

            searchAutoComplete.setOnItemClickListener { parent, _, position, _ ->
                val selectedWord = parent.getItemAtPosition(position) as String
                searchView.setQuery(selectedWord, true)
            }
        }
    }
}