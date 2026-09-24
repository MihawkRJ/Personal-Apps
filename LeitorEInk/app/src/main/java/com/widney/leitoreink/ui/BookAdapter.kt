/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.widney.leitoreink.data.Book
import com.widney.leitoreink.databinding.ItemBookBinding

class BookAdapter(
    private val onClick: (Book) -> Unit,
    private val onLongClick: (Book) -> Boolean
) : ListAdapter<Book, BookAdapter.BookViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.textTitle.text = book.displayName.removeSuffix(".${book.format.name.lowercase()}")
            binding.textFormatBadge.text = book.format.name
            binding.root.setOnClickListener { onClick(book) }
            binding.root.setOnLongClickListener { onLongClick(book) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
        }
    }
}
