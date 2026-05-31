package com.example.recipebookkotlin

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
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
import com.example.recipebookkotlin.Adapters.FolderActionListener
import com.example.recipebookkotlin.Adapters.FolderItem
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentFavorites : Fragment(), FolderActionListener {

    private lateinit var folderAdapter: FolderAdapter
    private lateinit var recyclerView: RecyclerView
    private var currentUsername: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        currentUsername = sharedPrefs.getString("USER_USERNAME", "") ?: ""

        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Ініціалізуємо адаптер та передаємо this як FolderActionListener
        folderAdapter = FolderAdapter(
            folders = emptyList(),
            onRecipeClick = { id ->
                val bundle = Bundle().apply { putLong("RECIPE_ID", id) }
                try {
                    findNavController().navigate(R.id.fragmentViewRecipe, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Перевір ID фрагмента у nav_graph!", Toast.LENGTH_LONG).show()
                }
            },
            actionListener = this
        )

        recyclerView.adapter = folderAdapter

        if (currentUsername.isNotEmpty()) {
            loadFavorites()
        } else {
            Toast.makeText(context, "Увійдіть в акаунт, щоб побачити закладки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFavorites() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val favorites = ApiClient.recipeApi.getFavorites(currentUsername)
                val groupedFavorites = favorites.groupBy { it.collectionName ?: "Улюблені" }

                val folderItems = groupedFavorites.map { (folderName, favList) ->
                    FolderItem(
                        name = folderName,
                        recipes = favList.map { it.recipe },
                        isExpanded = false
                    )
                }

                withContext(Dispatchers.Main) {
                    folderAdapter.updateData(folderItems)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не вдалося завантажити закладки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // МЕТОД: Перейменування папки
    override fun onEdit(folderName: String) {
        val editText = EditText(requireContext()).apply {
            setText(folderName)
            setPadding(50, 50, 50, 50)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Перейменувати папку")
            .setView(editText)
            .setPositiveButton("Зберегти") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != folderName) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            ApiClient.recipeApi.renameCollection(currentUsername, folderName, newName)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Папку перейменовано", Toast.LENGTH_SHORT).show()
                                loadFavorites() // Перезавантажуємо список
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Помилка", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    // МЕТОД: Видалення папки
    override fun onDelete(folderName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Видалення папки")
            .setMessage("Ви дійсно хочете видалити папку '$folderName'? Усі рецепти в ній будуть видалені з закладок.")
            .setPositiveButton("Видалити") { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        ApiClient.recipeApi.deleteCollection(currentUsername, folderName)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Папку видалено", Toast.LENGTH_SHORT).show()
                            loadFavorites() // Перезавантажуємо список
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Помилка", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }
}