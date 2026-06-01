package com.example.recipebookkotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.recipebookkotlin.dto.IngredientDTO
import com.example.recipebookkotlin.dto.RecipeDTO
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Додані імпорти для нового синтаксису OkHttp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class FragmentEditRecipe : Fragment() {

    private var recipeId: Long = -1L

    // UI Елементи
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvCategory: TextView
    private lateinit var etInstructions: EditText
    private lateinit var btnSave: Button

    // Інгредієнти
    private lateinit var etIngredientName: EditText
    private lateinit var etIngredientQuantity: EditText
    private lateinit var btnAddIngredient: ImageButton
    private lateinit var ingredientContainer: LinearLayout
    private val ingredientList = mutableListOf<IngredientDTO>()

    // Списки для категорій
    private var allCategoryNames = emptyArray<String>()
    private val selectedCategories = mutableListOf<String>()

    // Фото
    private lateinit var btnEditPhoto: ImageButton
    private lateinit var imagesContainer: LinearLayout
    private var selectedImageUris = mutableListOf<Uri>()
    private var existingImageUrls = mutableListOf<String>()

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateImagePreviews()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipeId = arguments?.getLong("RECIPE_ID", -1L) ?: -1L

        etTitle = view.findViewById(R.id.editTextTitle)
        etDescription = view.findViewById(R.id.editTextDescription)
        tvCategory = view.findViewById(R.id.textViewSelectCategory)
        etInstructions = view.findViewById(R.id.editTextInstructions)
        btnSave = view.findViewById(R.id.buttonSaveRecipe)

        etIngredientName = view.findViewById(R.id.ingredient_input)
        etIngredientQuantity = view.findViewById(R.id.ingredient_quantity_input)
        btnAddIngredient = view.findViewById(R.id.imageView_addIngredient)
        ingredientContainer = view.findViewById(R.id.ingredientListContainer)

        btnEditPhoto = view.findViewById(R.id.ImageButton_edit_photo)
        imagesContainer = view.findViewById(R.id.imagesEditPreviewContainer)

        loadCategories()

        btnEditPhoto.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        btnAddIngredient.setOnClickListener {
            val name = etIngredientName.text.toString().trim()
            val quantity = etIngredientQuantity.text.toString().trim()

            if (name.isNotEmpty() && quantity.isNotEmpty()) {
                val newIngredient = IngredientDTO(name = name, quantity = quantity)
                ingredientList.add(newIngredient)

                updateIngredientList()

                etIngredientName.text.clear()
                etIngredientQuantity.text.clear()
            } else {
                Toast.makeText(context, "Введіть назву та кількість", Toast.LENGTH_SHORT).show()
            }
        }

        if (recipeId != -1L) {
            loadRecipeData(recipeId)
        } else {
            Toast.makeText(requireContext(), "Помилка: ID рецепта не знайдено", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener { saveEditedRecipe() }
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categories = ApiClient.categoryApi.getCategory()
                allCategoryNames = categories.map { it.name }.toTypedArray()
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Не вдалося завантажити категорії: ${e.message}")
            }
        }

        tvCategory.setOnClickListener {
            if (allCategoryNames.isEmpty()) {
                Toast.makeText(context, "Категорії ще завантажуються...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val checkedItems = BooleanArray(allCategoryNames.size) { i ->
                selectedCategories.contains(allCategoryNames[i])
            }

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Виберіть категорії")
                .setMultiChoiceItems(allCategoryNames, checkedItems) { _, position, isChecked ->
                    val category = allCategoryNames[position]
                    if (isChecked) {
                        selectedCategories.add(category)
                    } else {
                        selectedCategories.remove(category)
                    }
                }
                .setPositiveButton("Зберегти") { _, _ ->
                    tvCategory.text = if (selectedCategories.isNotEmpty()) {
                        selectedCategories.joinToString(", ")
                    } else {
                        "Натисніть, щоб вибрати категорії"
                    }
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }
    }

    private fun loadRecipeData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recipe = ApiClient.recipeApi.getRecipeById(id)
                withContext(Dispatchers.Main) {
                    etTitle.setText(recipe.title)
                    etDescription.setText(recipe.description)
                    etInstructions.setText(recipe.instruction)

                    recipe.categoryNames?.let {
                        selectedCategories.clear()
                        selectedCategories.addAll(it)
                        tvCategory.text = selectedCategories.joinToString(", ")
                    }

                    recipe.ingredients?.let { ingredients ->
                        ingredientList.clear()
                        ingredientList.addAll(ingredients)
                        updateIngredientList()
                    }

                    recipe.imageUrls?.let { urls ->
                        existingImageUrls.clear()
                        existingImageUrls.addAll(urls)
                        updateImagePreviews()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Не вдалося завантажити дані", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateIngredientList() {
        ingredientContainer.removeAllViews()

        ingredientList.forEach { ingredient ->
            val itemView = layoutInflater.inflate(R.layout.ingredient_item, ingredientContainer, false)

            val nameText = itemView.findViewById<TextView>(R.id.ingredientName)
            val quantityText = itemView.findViewById<TextView>(R.id.ingredientQuantity)
            val deleteBtn = itemView.findViewById<ImageView>(R.id.deleteButton)

            nameText.text = ingredient.name
            quantityText.text = ingredient.quantity

            deleteBtn.setOnClickListener {
                ingredientList.remove(ingredient)
                updateIngredientList()
            }

            ingredientContainer.addView(itemView)
        }
    }

    private fun updateImagePreviews() {
        imagesContainer.removeAllViews()

        // Використовуємо toList(), щоб уникнути помилки при видаленні елементів під час перебору
        existingImageUrls.toList().forEach { url ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(8, 0, 8, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP

                val baseUrl = ApiClient.ipAdres.trimEnd('/')
                val fullUrl = "$baseUrl/uploads/$url"

                load(fullUrl) {
                    crossfade(true)
                    placeholder(R.drawable.icon_add_photo)
                    error(R.drawable.icon_add_photo)
                }
            }

            // Логіка видалення існуючого фото
            imageView.setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Видалити фото?")
                    .setPositiveButton("Так") { _, _ ->
                        existingImageUrls.remove(url)
                        updateImagePreviews() // Оновлюємо UI
                    }
                    .setNegativeButton("Ні", null)
                    .show()
            }

            imagesContainer.addView(imageView)
        }

        selectedImageUris.toList().forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(8, 0, 8, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }

            // Логіка видалення нового (тільки що вибраного) фото
            imageView.setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Видалити фото?")
                    .setPositiveButton("Так") { _, _ ->
                        selectedImageUris.remove(uri)
                        updateImagePreviews() // Оновлюємо UI
                    }
                    .setNegativeButton("Ні", null)
                    .show()
            }

            imagesContainer.addView(imageView)
        }
    }

    private fun saveEditedRecipe() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val instructions = etInstructions.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || selectedCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Заповніть назву, опис та виберіть категорію", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getLong("USER_ID", -1L)

        if (userId == -1L) {
            Toast.makeText(requireContext(), "Помилка: Перезайдіть в акаунт!", Toast.LENGTH_LONG).show()
            return
        }

        val updatedRecipe = RecipeDTO(
            id = recipeId,
            title = title,
            description = description,
            categoryNames = selectedCategories,
            instruction = instructions,
            ingredients = ingredientList,
            authorName = "",
            averageRating = 0.0,
            imageUrls = existingImageUrls, // Передаємо список старих фото, які залишилися
            votesCount = 0
        )

        // 1. Конвертуємо рецепт у JSON-рядок
        val gson = com.google.gson.Gson()
        val recipeJsonString = gson.toJson(updatedRecipe)

        // НОВИЙ СИНТАКСИС OkHttp 4.x для JSON
        val recipeRequestBody = recipeJsonString.toRequestBody("application/json".toMediaTypeOrNull())

        // 2. Готуємо НОВІ фотографії до відправки
        val newImageParts = mutableListOf<okhttp3.MultipartBody.Part>()

        selectedImageUris.forEach { uri ->
            val file = getFileFromUri(requireContext(), uri)
            if (file != null) {
                // НОВИЙ СИНТАКСИС OkHttp 4.x для Файлу
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = okhttp3.MultipartBody.Part.createFormData("newImages", file.name, requestFile)
                newImageParts.add(body)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 3. Відправляємо Multipart-запит на сервер
                val response = ApiClient.recipeApi.updateRecipeWithImages(
                    id = recipeId,
                    userId = userId,
                    recipeJson = recipeRequestBody,
                    newImages = if (newImageParts.isEmpty()) null else newImageParts
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Рецепт успішно оновлено!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Помилка сервера: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Збій: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Допоміжний метод для перетворення Uri (з галереї) у File (для відправки на сервер)
    private fun getFileFromUri(context: Context, uri: Uri): java.io.File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = java.io.File.createTempFile("upload_", ".jpg", context.cacheDir)
            val outputStream = java.io.FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}