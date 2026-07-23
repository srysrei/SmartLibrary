package com.smartlibrary.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.smartlibrary.R
import com.smartlibrary.network.ImageUrls
import com.smartlibrary.network.dto.BookResponse
import com.smartlibrary.ui.UiHelpers

class BookAdapter(
    private var books: List<BookResponse>,
    private val onItemClick: (BookResponse) -> Unit,
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivBookCover)
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val tvTitle: TextView = view.findViewById(R.id.tvBookTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = "by ${book.author}"
        holder.tvBadge.text = book.categoryName ?: "—"

        holder.tvStock.text = "● ${UiHelpers.stockText(book.availableQty)}"
        holder.tvStock.setTextColor(UiHelpers.stockColor(holder.itemView.context, book.availableQty))

        if (!book.imageUrl.isNullOrBlank()) {
            holder.ivCover.load(ImageUrls.resolve(book.imageUrl)) {
                crossfade(true)
                error(R.drawable.ic_book)
            }
        } else {
            holder.ivCover.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener { onItemClick(book) }
    }

    override fun getItemCount() = books.size

    fun updateBooks(newBooks: List<BookResponse>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
