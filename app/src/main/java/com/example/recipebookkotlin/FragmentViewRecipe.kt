package com.example.recipebookkotlin

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
    private var isFavorite: Boolean = false
    private var isAuthorOrAdmin: Boolean = false
    private var currentCollectionName: String = "Улюблені"

    // ДОДАНО: Зберігаємо список існуючих папок користувача
    private var existingFolders: List<String> = emptyList()

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

                val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
                val username = sharedPrefs.getString("USER_USERNAME", "") ?: ""
                val currentUserId = sharedPrefs.getLong("USER_ID", -1L)
                val isAdmin = sharedPrefs.getBoolean("IS_ADMIN", false)

                if (username.isNotEmpty()) {
                    try {
                        val favorites = ApiClient.recipeApi.getFavorites(username)

                        // Шукаємо, чи є рецепт в закладках
                        val foundFavorite = favorites.find { it.recipe.id == currentRecipeId }
                        isFavorite = foundFavorite != null
                        if (foundFavorite != null && foundFavorite.collectionName != null) {
                            currentCollectionName = foundFavorite.collectionName
                        }

                        // ДОДАНО: Витягуємо всі унікальні назви папок з сервера
                        existingFolders = favorites
                            .mapNotNull { it.collectionName }
                            .filter { it.isNotBlank() }
                            .distinct()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    isAuthorOrAdmin = (username == recipe.authorName) || isAdmin
                    populateUI(view, recipe, currentUserId)
                    setupFavoriteButton(view, username, recipe.id)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun populateUI(view: View, recipe: com.example.recipebookkotlin.dto.RecipeDTO, currentUserId: Long){
        view.findViewById<TextView>(R.id.textView_recipeTitle).text = recipe.title
        val categoriesText = recipe.categoryNames?.joinToString(", ")
        view.findViewById<TextView>(R.id.textView_recipeCategory).text = if (!categoriesText.isNullOrEmpty()) categoriesText else "Без категорії"
        view.findViewById<TextView>(R.id.textView_recipeAuthor).text = "Шеф: ${recipe.authorName}"
        view.findViewById<TextView>(R.id.textView_recipeDescription).text = recipe.description
        view.findViewById<TextView>(R.id.textView_recipeInstruction).text = recipe.instruction ?: "Інструкція відсутня"

        val ratingBar = view.findViewById<android.widget.RatingBar>(R.id.recipeRatingBar)
        val tvAverageRating = view.findViewById<TextView>(R.id.textView_averageRating)

        tvAverageRating.text = "Середній бал: ${recipe.averageRating}"

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        val userRatingKey = "RATING_${currentUserId}_RECIPE_${recipe.id}"
        val savedUserRating = sharedPrefs.getFloat(userRatingKey, 0f)
        ratingBar.rating = savedUserRating

        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                sharedPrefs.edit().putFloat(userRatingKey, rating).apply()

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val newAverage = ApiClient.recipeApi.rateRecipe(recipe.id, currentUserId, rating.toInt())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Дякуємо за оцінку!", Toast.LENGTH_SHORT).show()
                            tvAverageRating.text = "Середній бал: $newAverage"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Помилка відправки оцінки", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

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

        val imagesContainer = view.findViewById<LinearLayout>(R.id.imagesContainer)
        imagesContainer.removeAllViews()

        recipe.imageUrls?.forEach { imageUrl ->
            val imageView = ImageView(requireContext()).apply {
                val widthPx = (300 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(widthPx, LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0, 0, 24, 0) }
                scaleType = ImageView.ScaleType.FIT_CENTER
                val baseUrl = ipAdres.trimEnd('/')
                val fullUrl = "$baseUrl/uploads/$imageUrl"

                load(fullUrl) {
                    crossfade(true)
                    placeholder(R.drawable.icon_add_photo)
                    error(R.drawable.icon_add_photo)
                    transformations(coil.transform.RoundedCornersTransformation(40f))
                }
            }
            imagesContainer.addView(imageView)
        }

        val buttonEdit = view.findViewById<Button>(R.id.btnEdit)
        val buttonDelete = view.findViewById<Button>(R.id.btnDelete)

        if (isAuthorOrAdmin) {
            buttonEdit.visibility = View.VISIBLE
            buttonDelete.visibility = View.VISIBLE

            buttonEdit.setOnClickListener {
                val bundle = Bundle().apply { putLong("RECIPE_ID", recipe.id) }
                findNavController().navigate(R.id.fragment_edit_recipe, bundle)
            }

            buttonDelete.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Видалення рецепту")
                    .setMessage("Ви дійсно хочете видалити цей рецепт? Цю дію неможливо скасувати.")
                    .setPositiveButton("Видалити") { dialog, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val prefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
                            val userId = prefs.getLong("USER_ID", -1L)
                            try {
                                val response = ApiClient.recipeApi.deleteRecipe(recipe.id, userId)
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(requireContext(), "Рецепт видалено", Toast.LENGTH_SHORT).show()
                                        findNavController().popBackStack()
                                    } else {
                                        Toast.makeText(requireContext(), "Не вдалося видалити", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Скасувати") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        } else {
            buttonEdit.visibility = View.GONE
            buttonDelete.visibility = View.GONE
        }
    }

    private fun setupFavoriteButton(view: View, username: String, recipeId: Long) {
        val heartBtn = view.findViewById<ImageView>(R.id.buttonFavorite)
        heartBtn.setImageResource(if (isFavorite) R.drawable.icon_save_active else R.drawable.icon_save)

        heartBtn.setOnClickListener {
            if (username.isEmpty()) {
                Toast.makeText(context, "Будь ласка, увійдіть в акаунт", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isFavorite) {
                // Якщо рецепту немає в закладках - відкриваємо нове вікно вибору/створення папки
                showCollectionDialog(username, recipeId, heartBtn)
            } else {
                // Якщо є - видаляємо з тієї папки, де він лежить
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        ApiClient.recipeApi.removeFromFavorites(username, recipeId, currentCollectionName)
                        isFavorite = false
                        withContext(Dispatchers.Main) {
                            heartBtn.setImageResource(R.drawable.icon_save)
                            Toast.makeText(context, "Видалено з папки '$currentCollectionName'", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Помилка видалення", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // ПОВНІСТЮ ОНОВЛЕНИЙ МЕТОД ДІАЛОГУ
    private fun showCollectionDialog(username: String, recipeId: Long, heartBtn: ImageView) {
        // Завантажуємо наш новий красивий макет
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_favorite, null)
        val containerExistingFolders = dialogView.findViewById<LinearLayout>(R.id.containerExistingFolders)
        val editTextNewFolder = dialogView.findViewById<EditText>(R.id.editTextNewFolder)
        val buttonSaveNewFolder = dialogView.findViewById<Button>(R.id.buttonSaveNewFolder)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Робимо фон діалогу прозорим, щоб спрацювали закруглені кути з нашого xml
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Якщо в користувача ще немає папок, показуємо хоча б "Улюблені"
        val foldersToShow = if (existingFolders.isEmpty()) listOf("Улюблені") else existingFolders

        // Динамічно малюємо кнопки для кожної існуючої папки
        for (folder in foldersToShow) {
            val folderView = TextView(requireContext()).apply {
                text = "📁 $folder"
                textSize = 16f
                setPadding(0, 16, 0, 16) // Відступи між папками
                setTextColor(Color.parseColor("#1A1A1A"))

                // При натисканні на існуючу папку - одразу зберігаємо туди
                setOnClickListener {
                    saveToFolder(username, recipeId, folder, heartBtn, dialog)
                }
            }
            containerExistingFolders.addView(folderView)
        }

        // Логіка для створення НОВОЇ папки
        buttonSaveNewFolder.setOnClickListener {
            val newFolder = editTextNewFolder.text.toString().trim()
            if (newFolder.isNotEmpty()) {
                saveToFolder(username, recipeId, newFolder, heartBtn, dialog)
            } else {
                Toast.makeText(context, "Введіть назву нової папки", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // Виніс логіку збереження в окремий метод, щоб не дублювати код
    private fun saveToFolder(username: String, recipeId: Long, folderName: String, heartBtn: ImageView, dialog: AlertDialog) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ApiClient.recipeApi.addToFavorites(username, recipeId, folderName)
                isFavorite = true
                currentCollectionName = folderName

                // Додаємо нову папку до локального списку, щоб вона з'явилась при наступному натисканні
                if (!existingFolders.contains(folderName)) {
                    existingFolders = existingFolders + folderName
                }

                withContext(Dispatchers.Main) {
                    heartBtn.setImageResource(R.drawable.icon_save_active)
                    Toast.makeText(context, "Збережено в '$folderName'!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Помилка збереження", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}