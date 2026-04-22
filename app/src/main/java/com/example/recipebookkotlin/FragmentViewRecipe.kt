package com.example.recipebookkotlin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.recipebookkotlin.network.ApiClient
import com.example.recipebookkotlin.network.ApiClient.ipAdres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentViewRecipe : Fragment() {

    private var currentRecipeId: Long = -1L
    private var isFavorite: Boolean = false // ДОДАНО: Стан для сердечка

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentRecipeId = arguments?.getLong("RECIPE_ID") ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо кнопку і ставимо клік
        // val backButton = view.findViewById<ImageView>(R.id.imageView_back)
        // backButton.setOnClickListener {
        //     findNavController().popBackStack()
        // }

        if (currentRecipeId != -1L) {
            loadRecipeData(view)
        } else {
            Toast.makeText(requireContext(), "Помилка: Рецепт не знайдено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecipeData(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Завантажуємо сам рецепт
                val recipe = ApiClient.recipeApi.getRecipeById(currentRecipeId)

                // 2. ДОДАНО: Отримуємо username користувача
                val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
                val username = sharedPrefs.getString("USER_USERNAME", "") ?: ""

                // 3. ДОДАНО: Перевіряємо, чи є цей рецепт у закладках цього користувача
                if (username.isNotEmpty()) {
                    try {
                        val favorites = ApiClient.recipeApi.getFavorites(username)
                        // Перевіряємо, чи збігається ID рецепта з ID в закладках
                        isFavorite = favorites.any { it.recipe.id == currentRecipeId }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    populateUI(view, recipe)
                    // ДОДАНО: Налаштовуємо кнопку вподобань після завантаження даних
                    setupFavoriteButton(view, username, recipe.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("RECIPE_DEBUG", "Причина помилки: ", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun populateUI(view: View, recipe: com.example.recipebookkotlin.dto.RecipeDTO){
        // Текстові поля
        view.findViewById<TextView>(R.id.textView_recipeTitle).text = recipe.title
        view.findViewById<TextView>(R.id.textView_recipeCategory).text = recipe.categoryName
        view.findViewById<TextView>(R.id.textView_recipeAuthor).text = "Шеф: ${recipe.authorName}"
        view.findViewById<TextView>(R.id.textView_recipeDescription).text = recipe.description
        view.findViewById<TextView>(R.id.textView_recipeInstruction).text = recipe.instruction ?: "Інструкція відсутня"
        val ratingBar = view.findViewById<android.widget.RatingBar>(R.id.recipeRatingBar)

        ratingBar.rating = recipe.averageRating.toFloat()

        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val newAverage = ApiClient.recipeApi.rateRecipe(recipe.id, rating.toInt())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Дякуємо за оцінку!", Toast.LENGTH_SHORT).show()
                            ratingBar.rating = newAverage.toFloat()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Помилка відправки оцінки", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Інгредієнти
        val ingredientsContainer = view.findViewById<LinearLayout>(R.id.ingredientsContainer)
        ingredientsContainer.removeAllViews()

        recipe.ingredients.orEmpty().forEach { ingredient ->
            val textView = TextView(requireContext()).apply {
                text = "• ${ingredient.name} — ${ingredient.quantity}"
                setTextColor(resources.getColor(R.color.textColor, null))
                textSize = 16f
                setPadding(0, 4, 0, 4)
            }
            ingredientsContainer.addView(textView)
        }

        // Фотографії
        val imagesContainer = view.findViewById<LinearLayout>(R.id.imagesContainer)
        imagesContainer.removeAllViews()

        recipe.imageUrls?.forEach { imageUrl ->
            val imageView = ImageView(requireContext()).apply {
                val widthPx = (300 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    widthPx,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { setMargins(0, 0, 24, 0) }

                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                setBackgroundResource(R.drawable.background_edittext)

                val fullUrl = "$ipAdres/uploads/$imageUrl"
                load(fullUrl) {
                    crossfade(true)
                }
            }
            imagesContainer.addView(imageView)
        }
    }

    // ДОДАНО: Метод для роботи з кнопкою "Вподобати"
    private fun setupFavoriteButton(view: View, username: String, recipeId: Long) {
        val heartBtn = view.findViewById<ImageView>(R.id.buttonFavorite)

        // Встановлюємо правильну іконку при відкритті екрана
        heartBtn.setImageResource(if (isFavorite) R.drawable.icon_save_active else R.drawable.icon_save)

        heartBtn.setOnClickListener {
            if (username.isEmpty()) {
                Toast.makeText(context, "Будь ласка, увійдіть в акаунт", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (!isFavorite) {
                        ApiClient.recipeApi.addToFavorites(username, recipeId)
                        isFavorite = true
                    } else {
                        ApiClient.recipeApi.removeFromFavorites(username, recipeId)
                        isFavorite = false
                    }

                    withContext(Dispatchers.Main) {
                        // Оновлюємо іконку після успішного запиту
                        heartBtn.setImageResource(if (isFavorite) R.drawable.icon_save_active else R.drawable.icon_save)
                        Toast.makeText(context, if (isFavorite) "Додано до закладок!" else "Видалено з закладок", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Помилка синхронізації", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}