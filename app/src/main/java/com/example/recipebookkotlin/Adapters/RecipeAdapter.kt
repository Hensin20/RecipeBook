package com.example.recipebookkotlin.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.recipebookkotlin.R
import com.example.recipebookkotlin.dto.RecipeDTO
import com.example.recipebookkotlin.network.ApiClient.ipAdres

class RecipeAdapter(
    private var recipes: List<RecipeDTO>,
    private val onRecipeClick: (Long) -> Unit, // Дія при кліку на рецепт

    // Необов'язкові параметри для профілю (за замовчуванням null)
    private val onEditClick: ((Long) -> Unit)? = null,
    private val onDeleteClick: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textView_recipeTitle)
        val category: TextView = view.findViewById(R.id.textView_recipeCategory)
        val author: TextView = view.findViewById(R.id.textView_recipeAuthor)
        val image: ImageView = view.findViewById(R.id.imageView_recipeThumb)
        val rating: TextView = view.findViewById(R.id.textView_recipeRating)

        // Додаємо кнопки (використовуємо ImageButton?, щоб не було помилки, якщо їх немає в XML)
        val btnEdit: ImageButton? = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton? = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.title.text = recipe.title
        holder.category.text = recipe.categoryNames?.joinToString(", ") ?: "Без категорії"
        holder.author.text = recipe.authorName

        val formattedRating = String.format("%.1f", recipe.averageRating)
        holder.rating.text = "⭐ $formattedRating (${recipe.votesCount})"

        val firstImage = recipe.imageUrls?.firstOrNull()

        if (firstImage != null) {
            // trimEnd('/') забирає зайвий слеш з ipAdres, щоб не було помилки "//"
            val baseUrl = ipAdres.trimEnd('/')
            val fullUrl = "$baseUrl/uploads/$firstImage"

            holder.image.load(fullUrl) {
                crossfade(true)
                placeholder(R.drawable.icon_add_photo) // Показувати, поки вантажиться
                error(R.drawable.icon_add_photo)       // Показувати, якщо сервер видав помилку 404
            }
        } else {
            holder.image.setImageResource(R.drawable.icon_add_photo)
        }

        // 1. Клік на саму картку (перегляд рецепту)
        holder.itemView.setOnClickListener {
            onRecipeClick(recipe.id)
        }

        // 2. Логіка для кнопок "Редагувати" та "Видалити"
        if (onEditClick != null && onDeleteClick != null) {
            // Якщо функції передані (ми знаходимось у Профілі) -> Показуємо кнопки
            holder.btnEdit?.visibility = View.VISIBLE
            holder.btnDelete?.visibility = View.VISIBLE

            holder.btnEdit?.setOnClickListener {
                onEditClick.invoke(recipe.id)
            }
            holder.btnDelete?.setOnClickListener {
                onDeleteClick.invoke(recipe.id)
            }
        } else {
            // Якщо функції НЕ передані (ми на Головному екрані) -> Ховаємо кнопки
            holder.btnEdit?.visibility = View.GONE
            holder.btnDelete?.visibility = View.GONE
        }
    }

    override fun getItemCount() = recipes.size

    fun updateData(newRecipes: List<RecipeDTO>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}