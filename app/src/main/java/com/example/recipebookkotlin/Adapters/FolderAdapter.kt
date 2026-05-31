package com.example.recipebookkotlin.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.R
import com.example.recipebookkotlin.dto.RecipeDTO

data class FolderItem(
    val name: String,
    val recipes: List<RecipeDTO>,
    var isExpanded: Boolean = false
)

// НОВИЙ ІНТЕРФЕЙС
interface FolderActionListener {
    fun onEdit(folderName: String)
    fun onDelete(folderName: String)
}

class FolderAdapter(
    private var folders: List<FolderItem>,
    private val onRecipeClick: (Long) -> Unit,
    private val actionListener: FolderActionListener // ДОДАНО У КОНСТРУКТОР
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

        val recipeAdapter = RecipeAdapter(folder.recipes, onRecipeClick)
        holder.nestedRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.nestedRecyclerView.adapter = recipeAdapter

        val isExpanded = folder.isExpanded
        holder.nestedRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.arrow.rotation = if (isExpanded) 90f else 0f

        holder.header.setOnClickListener {
            folder.isExpanded = !folder.isExpanded
            notifyItemChanged(position)
        }

        // ДОДАНО: Логіка для кнопки меню
        holder.menuIcon.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Перейменувати")
            popup.menu.add("Видалити")
            popup.setOnMenuItemClickListener { item ->
                when(item.title) {
                    "Перейменувати" -> actionListener.onEdit(folder.name)
                    "Видалити" -> actionListener.onDelete(folder.name)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = folders.size

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: LinearLayout = itemView.findViewById(R.id.folderHeader)
        val folderName: TextView = itemView.findViewById(R.id.textViewFolderName)
        val recipeCount: TextView = itemView.findViewById(R.id.textViewRecipeCount)
        val arrow: ImageView = itemView.findViewById(R.id.imageViewArrow)
        val menuIcon: ImageView = itemView.findViewById(R.id.imageViewMenu) // ДОДАНО
        val nestedRecyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewFolderRecipes)
    }
}