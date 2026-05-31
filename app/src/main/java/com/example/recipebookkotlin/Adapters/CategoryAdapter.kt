package com.example.recipebookkotlin.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookkotlin.R

class CategoryAdapter(
    private var categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // Змінна для зберігання вибраної позиції (-1 означає, що нічого не вибрано)
    private var selectedPosition = -1

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

// ЛОГІКА ПІДСВІЧУВАННЯ
        if (position == selectedPosition) {
            holder.name.setBackgroundResource(R.drawable.bg_category_selected)
            holder.name.setTextColor(Color.WHITE)
        } else {
            // ЗМІНЕНО ОСЬ ТУТ:
            holder.name.setBackgroundResource(R.drawable.bg_category_default)
            holder.name.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.textColor))
        }

        // ОБРОБКА КЛІКУ
        holder.itemView.setOnClickListener {
            // Запам'ятовуємо попередню позицію, щоб зняти з неї виділення
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Оновлюємо тільки дві кнопки: стару і нову (це дає гарну анімацію)
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}