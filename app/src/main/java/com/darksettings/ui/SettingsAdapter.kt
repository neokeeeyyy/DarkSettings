package com.darksettings.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.darksettings.R

class SettingsAdapter(
    private val categories: List<SettingsCategory>,
    private val onClick: (SettingsCategory) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.title.text = category.title
        holder.summary.text = category.summary
        holder.icon.setImageResource(category.iconRes)

        val bg = holder.icon.background as? GradientDrawable
        bg?.setColor(ContextCompat.getColor(holder.itemView.context, category.colorRes))

        holder.itemView.setOnClickListener { onClick(category) }
    }

    override fun getItemCount() = categories.size
}
