package com.example.recipebookkotlin.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.R

// Тимчасово використовуємо String для категорій (назви)
class CategoryAdapter(
    private var categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textViewCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.name.text = category

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}