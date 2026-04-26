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

class FragmentEditRecipe : Fragment() {

    private var recipeId: Long = -1L

    // UI Елементи
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var etInstructions: EditText
    private lateinit var btnSave: Button

    // Інгредієнти
    private lateinit var etIngredientName: EditText
    private lateinit var etIngredientQuantity: EditText
    private lateinit var btnAddIngredient: ImageButton
    private lateinit var ingredientContainer: LinearLayout
    private val ingredientList = mutableListOf<IngredientDTO>()

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

        // Ініціалізація View
        etTitle = view.findViewById(R.id.editTextTitle)
        etDescription = view.findViewById(R.id.editTextDescription)
        etCategory = view.findViewById(R.id.editTextCategory)
        etInstructions = view.findViewById(R.id.editTextInstructions)
        btnSave = view.findViewById(R.id.buttonSaveRecipe)

        etIngredientName = view.findViewById(R.id.ingredient_input)
        etIngredientQuantity = view.findViewById(R.id.ingredient_quantity_input)
        btnAddIngredient = view.findViewById(R.id.imageView_addIngredient)
        ingredientContainer = view.findViewById(R.id.ingredientListContainer)

        btnEditPhoto = view.findViewById(R.id.ImageButton_edit_photo)
        imagesContainer = view.findViewById(R.id.imagesEditPreviewContainer)

        // 1. ЗАВАНТАЖУЄМО КАТЕГОРІЇ З БАЗИ
        loadCategories()

        // 2. НАЛАШТУВАННЯ КНОПКИ ФОТО
        btnEditPhoto.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        // 3. ДОДАВАННЯ НОВОГО ІНГРЕДІЄНТА
        btnAddIngredient.setOnClickListener {
            val name = etIngredientName.text.toString().trim()
            val quantity = etIngredientQuantity.text.toString().trim()

            if (name.isNotEmpty() && quantity.isNotEmpty()) {
                val newIngredient = IngredientDTO(name = name, quantity = quantity)
                ingredientList.add(newIngredient)
                addIngredientToUI(newIngredient)

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

    // НОВИЙ МЕТОД: Завантаження категорій з API
    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categories = ApiClient.categoryApi.getCategory()
                val names = categories.map { it.name }

                withContext(Dispatchers.Main) {
                    val context = context ?: return@withContext
                    val adapter = ArrayAdapter(context, R.layout.item_dropdown_category, names)
                    etCategory.setAdapter(adapter)

                    // Робимо так, щоб не можна було вводити текст вручну
                    etCategory.inputType = android.text.InputType.TYPE_NULL

                    etCategory.setOnClickListener {
                        etCategory.showDropDown()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Не вдалося завантажити категорії: ${e.message}")
            }
        }
    }

    private fun loadRecipeData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recipe = ApiClient.recipeApi.getRecipeById(id)
                withContext(Dispatchers.Main) {
                    etTitle.setText(recipe.title)
                    etDescription.setText(recipe.description)
                    // Важливо: filter = false, щоб AutoCompleteTextView просто відобразив текст без фільтрації списку
                    etCategory.setText(recipe.categoryName, false)
                    etInstructions.setText(recipe.instruction)

                    recipe.ingredients?.let { ingredients ->
                        ingredientList.clear()
                        ingredientList.addAll(ingredients)
                        ingredientContainer.removeAllViews()
                        ingredients.forEach { addIngredientToUI(it) }
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

    private fun addIngredientToUI(ingredient: IngredientDTO) {
        val textView = TextView(requireContext()).apply {
            text = "• ${ingredient.name} - ${ingredient.quantity} (Натисніть, щоб видалити)"
            textSize = 16f
            setTextColor(resources.getColor(R.color.editeText, null))
            setPadding(0, 8, 0, 8)

            setOnClickListener {
                ingredientList.remove(ingredient)
                ingredientContainer.removeView(this)
            }
        }
        ingredientContainer.addView(textView)
    }

    private fun updateImagePreviews() {
        imagesContainer.removeAllViews()

        existingImageUrls.forEach { url ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(8, 0, 8, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                load("${ApiClient.ipAdres}/uploads/$url")
            }
            imagesContainer.addView(imageView)
        }

        selectedImageUris.forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(8, 0, 8, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
            imagesContainer.addView(imageView)
        }
    }

    private fun saveEditedRecipe() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val instructions = etInstructions.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Заповніть обов'язкові поля", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getLong("USER_ID", -1L)

        if (userId == -1L) {
            Toast.makeText(requireContext(), "Помилка: USER_ID не знайдено. Перезайдіть в акаунт!", Toast.LENGTH_LONG).show()
            return
        }

        val updatedRecipe = RecipeDTO(
            id = recipeId,
            title = title,
            description = description,
            categoryName = category,
            instruction = instructions,
            ingredients = ingredientList,
            authorName = "",
            averageRating = 0.0,
            imageUrls = emptyList(),
            votesCount = 0
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.recipeApi.updateRecipe(recipeId, userId, updatedRecipe)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Рецепт оновлено!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Невідома помилка"
                        android.util.Log.e("SERVER_ERROR", "Код: ${response.code()}, Текст: $errorBody")
                        Toast.makeText(requireContext(), "Сервер відмовив. Код: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("NETWORK_ERROR", "Помилка: ${e.message}")
                    Toast.makeText(requireContext(), "Збій: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}