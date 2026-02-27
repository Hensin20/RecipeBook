package com.example.recipebookkotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import com.example.recipebookkotlin.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.clear
import kotlin.toString

class FragmentCreate : Fragment() {
    data class Ingredient(val name: String, val quantity: String)
    data class CookingStep(val text: String)

    private val ingredients = mutableListOf<Ingredient>()
    private val stepList = mutableListOf<CookingStep>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create, container, false)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Ingredient
        setupIngredientActions(view)
        loadIngredientData(view)

        //Category
        loadCategoryData(view)

    }

    private fun loadIngredientData(view: View){
        val nameInput = view.findViewById<AutoCompleteTextView>(R.id.ingredient_input)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rawIngredients = ApiClient.ingredientApi.getIngredients()
                val ingredientNames = rawIngredients.map { it.name }

                // ЛОГ ДЛЯ ПЕРЕВІРКИ
                android.util.Log.d("DEBUG_API", "Отримано інгредієнтів: ${ingredientNames.size}")
                android.util.Log.d("DEBUG_API", "Дані: $ingredientNames")

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ingredientNames)
                    nameInput.setAdapter(adapter)
                    // Примусово оновимо поріг
                    nameInput.threshold = 1
                }
            } catch (e: Exception) {
                android.util.Log.e("DEBUG_API", "Помилка мережі: ${e.message}")
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

                // Очищуємо поля для наступного введення
                nameInput.text.clear()
                quantityInput.text.clear()
                nameInput.requestFocus() // Повертаємо фокус на назву
            }
        }
    }

    private fun loadCategoryData(view: View) {
        val categoryInput = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categories = ApiClient.categoryApi.getCategory()
                val names = categories.map { it.name }

                withContext(Dispatchers.Main) {
                    val context = context ?: return@withContext
                    // Використовуємо правильну розмітку для випадаючого списку
                    val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, names)
                    categoryInput.setAdapter(adapter)

                    // Для Exposed Dropdown Menu важливо вимкнути введення тексту,
                    // щоб воно працювало як Spinner
                    categoryInput.inputType = android.text.InputType.TYPE_NULL

                    categoryInput.setOnClickListener {
                        categoryInput.showDropDown()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Categories failed: ${e.message}")
            }
        }
    }

    private fun updateIngredientList(container: LinearLayout) {
        container.removeAllViews()

        // Використання циклу по об'єктах безпечніше за індекси
        ingredients.forEach { ing ->
            val itemView = layoutInflater.inflate(R.layout.ingredient_item, container, false)
            val nameText = itemView.findViewById<TextView>(R.id.ingredientName)
            val quantityText = itemView.findViewById<TextView>(R.id.ingredientQuantity)
            val deleteBtn = itemView.findViewById<ImageView>(R.id.deleteButton)

            nameText.text = ing.name
            quantityText.text = ing.quantity

            deleteBtn.setOnClickListener {
                ingredients.remove(ing) // Видаляємо саме цей об'єкт
                updateIngredientList(container)
            }
            container.addView(itemView)
        }
    }

}