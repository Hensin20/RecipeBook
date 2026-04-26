package com.example.recipebookkotlin

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.Adapters.RecipeAdapter
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentFavorites : Fragment() {
    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. ОБОВ'ЯЗКОВО МАЄ БУТИ ЦЕЙ РЯДОК! Він шукає список у твоєму дизайні.
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)

        // 2. Тільки після того, як ми його знайшли, можемо налаштовувати:
        recyclerView.layoutManager = LinearLayoutManager(context)

        // ВИПРАВЛЕНО: Використовуємо іменований параметр onRecipeClick
        adapter = RecipeAdapter(
            recipes = emptyList(),
            onRecipeClick = { id ->
                val bundle = Bundle().apply { putLong("RECIPE_ID", id) }
                try {
                    findNavController().navigate(R.id.fragmentViewRecipe, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Перевір ID фрагмента у nav_graph!", Toast.LENGTH_LONG).show()
                }
            }
        )

        // 3. І тільки тепер ми можемо прикріпити адаптер
        recyclerView.adapter = adapter

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("USER_USERNAME", "") ?: ""

        if (username.isNotEmpty()) {
            loadFavorites(username)
        } else {
            Toast.makeText(context, "Увійдіть в акаунт, щоб побачити закладки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFavorites(username: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val favorites = ApiClient.recipeApi.getFavorites(username)
                val recipes = favorites.map { it.recipe }

                withContext(Dispatchers.Main) {
                    adapter.updateData(recipes)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не вдалося завантажити закладки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}