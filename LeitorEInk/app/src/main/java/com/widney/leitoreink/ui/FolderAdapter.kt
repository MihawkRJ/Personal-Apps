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
import com.widney.leitoreink.data.ScannedFolder
import com.widney.leitoreink.databinding.ItemFolderBinding

class FolderAdapter(
    private val onRemove: (ScannedFolder) -> Unit
) : ListAdapter<ScannedFolder, FolderAdapter.FolderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(private val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: ScannedFolder) {
            binding.textFolderName.text = folder.displayName
            binding.buttonRemoveFolder.setOnClickListener { onRemove(folder) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ScannedFolder>() {
            override fun areItemsTheSame(oldItem: ScannedFolder, newItem: ScannedFolder) =
                oldItem.uriString == newItem.uriString

            override fun areContentsTheSame(oldItem: ScannedFolder, newItem: ScannedFolder) =
                oldItem == newItem
        }
    }
}
