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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

        // ДОДАНО: Зчитуємо статус адміна
        val isAdmin = sharedPrefs.getBoolean("IS_ADMIN", false)

        // Відображення даних
        val textViewUsername = view.findViewById<TextView>(R.id.profileUsername)

        // ДОДАНО: Логіка відображення приставки (Адмін)
        if (isAdmin) {
            textViewUsername.text = "$username (Адмін)"
        } else {
            textViewUsername.text = username
        }

        view.findViewById<TextView>(R.id.profileEmail).text = email

        // Налаштування списку
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMyRecipes)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // ВИПРАВЛЕНО: Додано всі 3 дії (Перегляд, Редагування, Видалення)
        adapter = RecipeAdapter(
            recipes = emptyList(),
            onRecipeClick = { recipeId ->
                // Перехід до перегляду рецепту
                val bundle = Bundle().apply { putLong("RECIPE_ID", recipeId) }
                findNavController().navigate(R.id.fragmentViewRecipe, bundle)
            },
            onEditClick = { recipeId ->
                val bundle = Bundle().apply { putLong("RECIPE_ID", recipeId) }
                findNavController().navigate(R.id.fragment_edit_recipe, bundle)
                Toast.makeText(requireContext(), "Відкриваємо редагування: $recipeId", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { recipeId ->
                // Виклик діалогового вікна для підтвердження видалення
                showDeleteConfirmationDialog(recipeId, username)
            }
        )

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

    // ЛОГІКА ВИДАЛЕННЯ: Діалогове вікно
    private fun showDeleteConfirmationDialog(recipeId: Long, username: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Видалення рецепту")
            .setMessage("Ви дійсно хочете назавжди видалити цей рецепт?")
            .setPositiveButton("Видалити") { _, _ ->
                deleteRecipe(recipeId, username)
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    // ЛОГІКА ВИДАЛЕННЯ: Запит на сервер
    private fun deleteRecipe(recipeId: Long, username: String) {
        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)

        // Дістаємо ID поточного користувача.
        val userId = sharedPrefs.getLong("USER_ID", -1L)

        if (userId == -1L) {
            Toast.makeText(requireContext(), "Помилка авторизації (немає ID)", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.recipeApi.deleteRecipe(recipeId, userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Рецепт видалено", Toast.LENGTH_SHORT).show()
                        // Оновлюємо список після успішного видалення
                        loadMyRecipes(username)
                    } else {
                        Toast.makeText(requireContext(), "Помилка видалення на сервері", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка мережі", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun logoutUser() {
        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}