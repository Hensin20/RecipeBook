package com.example.recipebookkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentRecipeId = arguments?.getLong("RECIPE_ID") ?: -1L
    }

    // ДОДАЙ ЦЕЙ БЛОК: Він "надуває" (inflate) твій XML-дизайн
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ... твій існуючий код ...

        // Знаходимо кнопку і ставимо клік
        val backButton = view.findViewById<ImageView>(R.id.imageView_back)
        backButton.setOnClickListener {
            // Ця команда робить ТОЧНО ТЕ САМЕ, що й кнопка "Назад" на телефоні
            findNavController().popBackStack()
        }

        // Твій існуючий код завантаження...
        if (currentRecipeId != -1L) {
            loadRecipeData(view)
        } else {
            Toast.makeText(requireContext(), "Помилка: Рецепт не знайдено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecipeData(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recipe = ApiClient.recipeApi.getRecipeById(currentRecipeId)

                withContext(Dispatchers.Main) {
                    populateUI(view, recipe)
                }
            } catch (e: Exception) {
                // ДОДАЙ ЦЕЙ РЯДОК:
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
            // fromUser = true означає, що це натиснула людина, а не код вище поставив значення
            if (fromUser) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Відправляємо оцінку на сервер
                        val newAverage = ApiClient.recipeApi.rateRecipe(recipe.id, rating.toInt())

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Дякуємо за оцінку!", Toast.LENGTH_SHORT).show()
                            // Оновлюємо зірочки на нове середнє значення
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

        // Інгредієнти (динамічно додаємо у список)
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
                // Переводимо 300dp у пікселі для поточного екрана
                val widthPx = (300 * resources.displayMetrics.density).toInt()

                // Змінюємо MATCH_PARENT на widthPx
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
}