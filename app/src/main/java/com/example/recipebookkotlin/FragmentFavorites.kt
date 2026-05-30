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
import com.example.recipebookkotlin.Adapters.FolderAdapter
import com.example.recipebookkotlin.Adapters.FolderItem
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentFavorites : Fragment() {

    // ОНОВЛЕНО: Тепер використовуємо FolderAdapter замість RecipeAdapter
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Ініціалізуємо адаптер папок
        folderAdapter = FolderAdapter(
            folders = emptyList(),
            onRecipeClick = { id ->
                val bundle = Bundle().apply { putLong("RECIPE_ID", id) }
                try {
                    findNavController().navigate(R.id.fragmentViewRecipe, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Перевір ID фрагмента у nav_graph!", Toast.LENGTH_LONG).show()
                }
            }
        )

        recyclerView.adapter = folderAdapter

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
                // Отримуємо список закладок з сервера (у кожній є collectionName та Recipe)
                val favorites = ApiClient.recipeApi.getFavorites(username)

                // 🌟 НАЙГОЛОВНІША МАГІЯ: Групуємо рецепти по папках!
                // Якщо collectionName порожній або null, кладемо в папку "Улюблені"
                val groupedFavorites = favorites.groupBy { it.collectionName ?: "Улюблені" }

                // Перетворюємо словник у список об'єктів FolderItem
                val folderItems = groupedFavorites.map { (folderName, favList) ->
                    FolderItem(
                        name = folderName,
                        recipes = favList.map { it.recipe }, // Витягуємо самі рецепти
                        isExpanded = false // За замовчуванням папки згорнуті
                    )
                }

                withContext(Dispatchers.Main) {
                    // Оновлюємо дані в адаптері
                    folderAdapter.updateData(folderItems)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не вдалося завантажити закладки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}