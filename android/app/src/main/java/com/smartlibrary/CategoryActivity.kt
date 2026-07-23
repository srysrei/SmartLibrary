package com.smartlibrary

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smartlibrary.adapter.CategoryAdapter
import com.smartlibrary.data.repository.CategoryRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.dto.CategoryResponse
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var categoryRepo: CategoryRepository
    private lateinit var adapter: CategoryAdapter
    private lateinit var etName: EditText
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        categoryRepo = CategoryRepository(this)
        etName = findViewById(R.id.etName)
        progress = findViewById(R.id.progress)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnAdd).setOnClickListener { addCategory() }

        adapter = CategoryAdapter(
            emptyList(),
            onEdit = { confirmEdit(it) },
            onDelete = { confirmDelete(it) },
        )
        findViewById<RecyclerView>(R.id.rvCategories).apply {
            layoutManager = LinearLayoutManager(this@CategoryActivity)
            adapter = this@CategoryActivity.adapter
        }

        loadCategories()
    }

    private fun loadCategories() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = categoryRepo.fetchCategories()) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    adapter.update(r.data)
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun addCategory() {
        val name = etName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            toast(getString(R.string.category_enter_name))
            return
        }
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = categoryRepo.createCategory(name)) {
                is ApiResult.Success -> {
                    etName.text = null
                    toast(getString(R.string.category_created))
                    loadCategories()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun confirmEdit(c: CategoryResponse) {
        val input = EditText(this).apply {
            setText(c.name)
            setSelection(text.length)
            hint = getString(R.string.category_name_hint)
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.category_edit_title))
            .setView(input, pad, pad / 2, pad, 0)
            .setPositiveButton(getString(R.string.category_save)) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) toast(getString(R.string.category_enter_name)) else doEdit(c, name)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun doEdit(c: CategoryResponse, name: String) {
        if (name == c.name) return
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = categoryRepo.updateCategory(c.id, name)) {
                is ApiResult.Success -> {
                    toast(getString(R.string.category_updated))
                    loadCategories()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun confirmDelete(c: CategoryResponse) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.category_delete_title))
            .setMessage(getString(R.string.category_delete_message, c.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> doDelete(c) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun doDelete(c: CategoryResponse) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = categoryRepo.deleteCategory(c.id)) {
                is ApiResult.Success -> {
                    toast(getString(R.string.category_deleted))
                    loadCategories()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
