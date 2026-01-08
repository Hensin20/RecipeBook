package com.example.recipebookkotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.util.Consumer

class FragmentCreate : Fragment() {

    private val ingredients = mutableListOf<Ingredient>()

    data class Ingredient(val name: String, val quantity: String)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameInput = view.findViewById<EditText>(R.id.ingredient_input)
        val quantityInput = view.findViewById<EditText>(R.id.ingredient_quantity_input)
        val addButton = view.findViewById<ImageView>(R.id.imageView_addIngredient)
        val listContainer = view.findViewById<LinearLayout>(R.id.ingredientListContainer)

        addButton.setOnClickListener {
            val name = nameInput.text.toString()
            val quantity = quantityInput.text.toString()

            if(name.isNotBlank() && quantity.isNotBlank()){
                ingredients.add(Ingredient(name, quantity))
                updateIngredientList(listContainer)
                quantityInput.text.clear()
            }
        }
    }
    private fun updateIngredientList(container: LinearLayout){
        container.removeAllViews()

        for ((index,ing) in ingredients.withIndex()){
            val itemView = layoutInflater.inflate(R.layout.ingredient_item, container, false)
            val nameText = itemView.findViewById<TextView>(R.id.ingredientName)
            val quantityText = itemView.findViewById<TextView>(R.id.ingredientQuantity)
            val deleteBtn = itemView.findViewById<ImageView>(R.id.deleteButton)

            nameText.text = ing.name
            quantityText.text = ing.quantity

            deleteBtn.setOnClickListener {
                ingredients.removeAt(index)
                updateIngredientList(container)
            }
            container.addView(itemView)
        }

    }

}