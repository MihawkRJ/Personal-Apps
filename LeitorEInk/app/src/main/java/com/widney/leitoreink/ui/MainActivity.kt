/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.widney.leitoreink.EInkReaderApp
import com.widney.leitoreink.R
import com.widney.leitoreink.data.Book
import com.widney.leitoreink.data.LibraryRepository
import com.widney.leitoreink.data.ScannedFolder
import com.widney.leitoreink.databinding.ActivityMainBinding
import com.widney.leitoreink.databinding.DialogFoldersBinding
import com.widney.leitoreink.scan.LibraryScanner
import com.widney.leitoreink.util.AppTheme
import com.widney.leitoreink.util.ThemePrefs
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: LibraryRepository
    private lateinit var adapter: BookAdapter

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                binding.swipeRefresh.isRefreshing = true
                repository.addFolder(uri)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch { repository.addSingleFile(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val app = application as EInkReaderApp
        repository = LibraryRepository(this, app.database)

        adapter = BookAdapter(
            onClick = { book -> openReader(book) },
            onLongClick = { book -> confirmRemove(book); true }
        )
        binding.recyclerBooks.layoutManager = GridLayoutManager(this, spanCountForWidth())
        binding.recyclerBooks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refreshLibrary() }

        lifecycleScope.launch {
            repository.observeBooks().collect { books ->
                adapter.submitList(books)
                val isEmpty = books.isEmpty()
                binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.recyclerBooks.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    private fun spanCountForWidth(): Int {
        val widthDp = resources.configuration.screenWidthDp
        return (widthDp / 110).coerceAtLeast(2)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_folder -> {
                pickFolder.launch(null)
                true
            }
            R.id.action_add_file -> {
                pickFile.launch(LibraryScanner.SUPPORTED_MIME_TYPES)
                true
            }
            R.id.action_refresh -> {
                refreshLibrary()
                true
            }
            R.id.action_manage_folders -> {
                showFoldersDialog()
                true
            }
            R.id.action_theme -> {
                showThemeDialog()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshLibrary() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            repository.rescanAll()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun openReader(book: Book) {
        val intent = Intent(this, ReaderActivity::class.java)
        intent.putExtra(ReaderActivity.EXTRA_BOOK_ID, book.id)
        startActivity(intent)
    }

    private fun confirmRemove(book: Book) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remove_book)
            .setMessage(getString(R.string.remove_book_confirm, book.displayName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                lifecycleScope.launch { repository.removeBook(book) }
            }
            .show()
    }

    private fun showFoldersDialog() {
        val dialogBinding = DialogFoldersBinding.inflate(LayoutInflater.from(this))
        val folderAdapter = FolderAdapter { folder -> confirmRemoveFolder(folder) }
        dialogBinding.recyclerFolders.layoutManager = LinearLayoutManager(this)
        dialogBinding.recyclerFolders.adapter = folderAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.folders_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.close, null)
            .create()

        // Job próprio (não o do ciclo de vida da Activity) para não continuar
        // coletando a lista depois que o diálogo já foi fechado.
        val collectJob = lifecycleScope.launch {
            repository.observeFolders().collect { folders ->
                folderAdapter.submitList(folders)
                dialogBinding.textNoFolders.visibility =
                    if (folders.isEmpty()) View.VISIBLE else View.GONE
                dialogBinding.recyclerFolders.visibility =
                    if (folders.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        dialog.setOnDismissListener { collectJob.cancel() }

        dialog.show()
    }

    private fun showThemeDialog() {
        val current = ThemePrefs.current(this)
        val options = arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark))
        val checkedItem = if (current == AppTheme.DARK) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_theme)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val chosen = if (which == 1) AppTheme.DARK else AppTheme.LIGHT
                if (chosen != current) {
                    ThemePrefs.setTheme(this, chosen)
                    dialog.dismiss()
                    recreate()
                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showAboutDialog() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_title)
            .setMessage(getString(R.string.about_message, version))
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun confirmRemoveFolder(folder: ScannedFolder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_manage_folders)
            .setMessage(getString(R.string.remove_folder_confirm, folder.displayName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                lifecycleScope.launch { repository.removeFolder(folder) }
            }
            .show()
    }
}
