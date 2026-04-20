package com.example.recipebookkotlin.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.recipebookkotlin.R
import com.example.recipebookkotlin.dto.RecipeDTO
import com.example.recipebookkotlin.network.ApiClient.ipAdres

class RecipeAdapter(
    private var recipes: List<RecipeDTO>,
    private val onRecipeClick: (Long) -> Unit // Дія при кліку на рецепт
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textView_recipeTitle)
        val category: TextView = view.findViewById(R.id.textView_recipeCategory)
        val author: TextView = view.findViewById(R.id.textView_recipeAuthor)
        val image: ImageView = view.findViewById(R.id.imageView_recipeThumb)
        val rating: TextView = view.findViewById(R.id.textView_recipeRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.title.text = recipe.title
        holder.category.text = recipe.categoryName
        holder.author.text = recipe.authorName

        val formattedRating = String.format("%.1f", recipe.averageRating)
        // Виводимо текст у форматі: ⭐ 4.5 (12)
        holder.rating.text = "⭐ $formattedRating (${recipe.votesCount})"
        val firstImage = recipe.imageUrls?.firstOrNull()

        if (firstImage != null) {
            val fullUrl = "$ipAdres/uploads/$firstImage"
            holder.image.load(fullUrl) {
                crossfade(true)
            }
        } else {
            holder.image.setImageResource(R.drawable.icon_add_photo)
        }

        holder.itemView.setOnClickListener {
            onRecipeClick(recipe.id)
        }
    }

    override fun getItemCount() = recipes.size

    fun updateData(newRecipes: List<RecipeDTO>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}