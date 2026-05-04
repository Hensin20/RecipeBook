package com.example.recipebookkotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recipebookkotlin.dto.IngredientDTO
import com.example.recipebookkotlin.dto.RecipeCreateDTO
import com.example.recipebookkotlin.network.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class FragmentCreate : Fragment() {
    data class Ingredient(val name: String, val quantity: String)

    private val ingredients = mutableListOf<Ingredient>()

    // ДОДАНО: Списки для мульти-вибору категорій
    private var allCategoryNames = emptyArray<String>()
    private val selectedCategories = mutableListOf<String>()

    private val selectedImageList = mutableListOf<Uri>()
    private val pickMultipleImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if(uris.isNotEmpty()){
            selectedImageList.clear()
            selectedImageList.addAll(uris)
            updateImagePreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val addPhotoButton = view.findViewById<ImageView>(R.id.ImageButton_add_photo)
        addPhotoButton.setOnClickListener {
            pickMultipleImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        val postButton = view.findViewById<android.widget.Button>(R.id.buttonLogin)
        postButton.setOnClickListener {
            saveRecipe(view)
        }

        // Ingredient
        setupIngredientActions(view)
        loadIngredientData(view)

        // Category (ОНОВЛЕНО)
        loadCategoryData(view)
    }

    private fun saveRecipe(view: View) {
        val title = view.findViewById<EditText>(R.id.editTextText_title).text.toString().trim()
        val description = view.findViewById<EditText>(R.id.editTextText_description).text.toString().trim()
        val instruction = view.findViewById<EditText>(R.id.editTextText_instruction).text.toString().trim()

        // ПЕРЕВІРКА: чи вибрали хоч одну категорію
        if (title.isEmpty() || selectedCategories.isEmpty() || ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "Заповніть назву, виберіть категорію та додайте інгредієнти!", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPrefs = requireContext().getSharedPreferences("RecipeBookPrefs", Context.MODE_PRIVATE)
        val currentUsername = sharedPrefs.getString("USER_USERNAME", "Анонім") ?: "Анонім"

        val recipeDTO = RecipeCreateDTO(
            title = title,
            description = description,
            categoryNames = selectedCategories, // ОНОВЛЕНО: передаємо СПИСОК категорій
            authorName = currentUsername,
            instruction = instruction,
            ingredients = ingredients.map { IngredientDTO(it.name, it.quantity) }
        )

        val jsonRecipe = Gson().toJson(recipeDTO)
        val recipeRequestBody = jsonRecipe.toRequestBody("application/json".toMediaTypeOrNull())
        val imagesParts = selectedImageList.mapNotNull { uri -> prepareFilePart(uri) }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ApiClient.recipeApi.createRecipe(recipeRequestBody, imagesParts)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Рецепт успішно створено!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка при збереженні", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun prepareFilePart(uri: Uri): MultipartBody.Part? {
        val context = context ?: return null
        return try {
            val file = File(context.cacheDir, "recipe_image_${System.currentTimeMillis()}.jpg")
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("images", file.name, requestFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateImagePreview(){
        val container = view?.findViewById<LinearLayout>(R.id.imagesPreviewContainer) ?:return
        container.removeAllViews()

        selectedImageList.forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(250,250).apply {
                    setMargins(10,0,10,0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource((R.drawable.background_edittext))
                clipToOutline = true
                load(uri){
                    crossfade(true)
                    size (300,300)
                }
                setOnClickListener {
                    selectedImageList.remove(uri)
                    updateImagePreview()
                }
            }
            container.addView(imageView)
        }
    }

    private fun loadIngredientData(view: View){
        val nameInput = view.findViewById<AutoCompleteTextView>(R.id.ingredient_input)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rawIngredients = ApiClient.ingredientApi.getIngredients()
                val ingredientNames = rawIngredients.map { it.name }

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ingredientNames)
                    nameInput.setAdapter(adapter)
                    nameInput.threshold = 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupIngredientActions(view: View) {
        val nameInput = view.findViewById<AutoCompleteTextView>(R.id.ingredient_input)
        val quantityInput = view.findViewById<EditText>(R.id.ingredient_quantity_input)
        val addButton = view.findViewById<ImageView>(R.id.imageView_addIngredient)
        val listContainer = view.findViewById<LinearLayout>(R.id.ingredientListContainer)

        addButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val quantity = quantityInput.text.toString().trim()

            if (name.isNotEmpty() && quantity.isNotEmpty()) {
                val newIngredient = Ingredient(name, quantity)
                ingredients.add(newIngredient)
                updateIngredientList(listContainer)

                nameInput.text.clear()
                quantityInput.text.clear()
                nameInput.requestFocus()
            }
        }
    }

    // ОНОВЛЕНО: Логіка для вибору КІЛЬКОХ категорій
    private fun loadCategoryData(view: View) {
        val tvCategory = view.findViewById<TextView>(R.id.textViewSelectCategory)

        // 1. Завантажуємо категорії з сервера
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categories = ApiClient.categoryApi.getCategory()
                allCategoryNames = categories.map { it.name }.toTypedArray()
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Categories failed: ${e.message}")
            }
        }

        // 2. Відкриваємо діалог при натисканні
        tvCategory.setOnClickListener {
            if (allCategoryNames.isEmpty()) {
                Toast.makeText(context, "Категорії ще завантажуються...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Масив галочок (true, якщо категорія вже є в нашому списку selectedCategories)
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
                    // Виводимо вибрані категорії на екран (через кому)
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

    private fun updateIngredientList(container: LinearLayout) {
        container.removeAllViews()
        ingredients.forEach { ing ->
            val itemView = layoutInflater.inflate(R.layout.ingredient_item, container, false)
            val nameText = itemView.findViewById<TextView>(R.id.ingredientName)
            val quantityText = itemView.findViewById<TextView>(R.id.ingredientQuantity)
            val deleteBtn = itemView.findViewById<ImageView>(R.id.deleteButton)

            nameText.text = ing.name
            quantityText.text = ing.quantity

            deleteBtn.setOnClickListener {
                ingredients.remove(ing)
                updateIngredientList(container)
            }
            container.addView(itemView)
        }
    }
}