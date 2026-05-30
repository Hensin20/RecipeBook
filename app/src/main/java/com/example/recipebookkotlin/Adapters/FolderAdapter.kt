package com.example.recipebookkotlin.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.R
import com.example.recipebookkotlin.dto.RecipeDTO

// Допоміжна модель для папки
data class FolderItem(
    val name: String,
    val recipes: List<RecipeDTO>,
    var isExpanded: Boolean = false // Чи відкрита папка
)

class FolderAdapter(
    private var folders: List<FolderItem>,
    private val onRecipeClick: (Long) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    fun updateData(newFolders: List<FolderItem>) {
        folders = newFolders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]

        holder.folderName.text = folder.name
        holder.recipeCount.text = "${folder.recipes.size} рецептів"

        // Налаштовуємо внутрішній список рецептів
        val recipeAdapter = RecipeAdapter(folder.recipes, onRecipeClick)
        holder.nestedRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.nestedRecyclerView.adapter = recipeAdapter

        // Встановлюємо стан (відкрито/закрито)
        val isExpanded = folder.isExpanded
        holder.nestedRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Анімація стрілочки (якщо відкрито - дивиться вниз, закрито - вправо)
        holder.arrow.rotation = if (isExpanded) 90f else 0f

        // Логіка кліку по шапці папки
        holder.header.setOnClickListener {
            folder.isExpanded = !folder.isExpanded
            notifyItemChanged(position) // Оновлюємо тільки цю папку
        }
    }

    override fun getItemCount(): Int = folders.size

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: LinearLayout = itemView.findViewById(R.id.folderHeader)
        val folderName: TextView = itemView.findViewById(R.id.textViewFolderName)
        val recipeCount: TextView = itemView.findViewById(R.id.textViewRecipeCount)
        val arrow: ImageView = itemView.findViewById(R.id.imageViewArrow)
        val nestedRecyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewFolderRecipes)
    }
}