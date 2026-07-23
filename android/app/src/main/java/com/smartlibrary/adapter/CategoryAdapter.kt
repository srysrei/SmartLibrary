package com.smartlibrary.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartlibrary.R
import com.smartlibrary.network.dto.CategoryResponse

class CategoryAdapter(
    private var items: List<CategoryResponse>,
    private val onEdit: (CategoryResponse) -> Unit,
    private val onDelete: (CategoryResponse) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.tvName.text = c.name
        holder.tvMeta.text = "CAT_ID ${c.id} · ${c.bookCount} books"
        holder.btnEdit.setOnClickListener { onEdit(c) }
        holder.btnDelete.setOnClickListener { onDelete(c) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<CategoryResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
