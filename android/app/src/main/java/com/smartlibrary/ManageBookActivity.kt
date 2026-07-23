package com.smartlibrary

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.smartlibrary.data.repository.BookRepository
import com.smartlibrary.data.repository.CategoryRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.ImageUrls
import com.smartlibrary.network.dto.BookRequest
import com.smartlibrary.network.dto.CategoryResponse
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ManageBookActivity : AppCompatActivity() {

    private lateinit var bookRepo: BookRepository
    private lateinit var categoryRepo: CategoryRepository

    private lateinit var etTitle: TextInputEditText
    private lateinit var etAuthor: TextInputEditText
    private lateinit var acCategory: MaterialAutoCompleteTextView
    private lateinit var etDescription: TextInputEditText
    private lateinit var ivCoverPreview: ImageView
    private lateinit var tvRemoveImage: TextView
    private lateinit var etQty: TextInputEditText
    private lateinit var progress: ProgressBar

    private var categories: List<CategoryResponse> = emptyList()
    private var selectedCategoryId: Long? = null
    private var imageUri: String? = null
    // The cover the book already had when editing, so we can preserve it while a new upload runs.
    private var originalImageUrl: String? = null
    private var bookId: Long = -1L
    private var isEdit = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Keep read access across restarts so the cover still renders later.
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            setImage(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_book)

        bookRepo = BookRepository(this)
        categoryRepo = CategoryRepository(this)

        etTitle = findViewById(R.id.etTitle)
        etAuthor = findViewById(R.id.etAuthor)
        acCategory = findViewById(R.id.acCategory)
        etDescription = findViewById(R.id.etDescription)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        tvRemoveImage = findViewById(R.id.tvRemoveImage)
        etQty = findViewById(R.id.etQty)
        progress = findViewById(R.id.progress)

        bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        isEdit = bookId > 0
        if (isEdit) findViewById<TextView>(R.id.tvTitle).text = getString(R.string.managebook_edit_book)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvManageCategories).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }

        ivCoverPreview.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        tvRemoveImage.setOnClickListener { setImage(null) }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            categories = when (val r = categoryRepo.fetchCategories()) {
                is ApiResult.Success -> r.data
                is ApiResult.Error -> categoryRepo.cachedCategories().map {
                    CategoryResponse(it.id, it.name, it.bookCount.toLong())
                }
            }
            val names = categories.map { it.name }
            acCategory.setAdapter(
                ArrayAdapter(this@ManageBookActivity, android.R.layout.simple_list_item_1, names)
            )
            acCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
            }
            // Keep prior selection label if editing.
            selectedCategoryId?.let { id ->
                categories.firstOrNull { it.id == id }?.let { acCategory.setText(it.name, false) }
            }
            if (isEdit && bookId > 0 && etTitle.text.isNullOrEmpty()) loadBook()
        }
    }

    private fun loadBook() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = bookRepo.getBook(bookId)) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    val b = r.data
                    etTitle.setText(b.title)
                    etAuthor.setText(b.author)
                    etDescription.setText(b.description)
                    originalImageUrl = b.imageUrl
                    setImage(b.imageUrl)
                    etQty.setText(b.qty.toString())
                    selectedCategoryId = b.categoryId
                    categories.firstOrNull { it.id == b.categoryId }?.let {
                        acCategory.setText(it.name, false)
                    } ?: b.categoryName?.let { acCategory.setText(it, false) }
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun save() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val author = etAuthor.text?.toString()?.trim().orEmpty()
        val description = etDescription.text?.toString()?.trim().orEmpty()
        val cover = imageUri?.trim().orEmpty()
        val qty = etQty.text?.toString()?.trim()?.toIntOrNull()

        if (title.isEmpty() || author.isEmpty() || description.isEmpty()) {
            toast(getString(R.string.managebook_error_required))
            return
        }
        if (selectedCategoryId == null) {
            toast(getString(R.string.managebook_error_select_category))
            return
        }
        if (qty == null || qty < 0) {
            toast(getString(R.string.managebook_error_valid_quantity))
            return
        }

        // A freshly picked cover is a local content:// / file:// URI that must be uploaded to the
        // book after it exists. An unchanged existing cover is a server path we send through as-is.
        val isLocalPick = cover.startsWith("content:", true) || cover.startsWith("file:", true)
        val bodyImageUrl: String? = when {
            // Keep the current cover on the record while the new file uploads (null when creating).
            isLocalPick -> originalImageUrl
            // Unchanged existing cover, or null if the user removed it (the server deletes the file).
            else -> cover.ifBlank { null }
        }

        val body = BookRequest(
            title = title,
            author = author,
            categoryId = selectedCategoryId!!,
            description = description,
            qty = qty,
            imageUrl = bodyImageUrl,
        )

        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            // Step 1 — create/update the book so we have an id to attach the cover to.
            val saved = if (isEdit) bookRepo.updateBook(bookId, body) else bookRepo.createBook(body)
            val savedBook = when (saved) {
                is ApiResult.Success -> saved.data
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(saved.message)
                    return@launch
                }
            }

            // Step 2 — if a new local cover was picked, upload it to POST books/{id}/image.
            var imageOk = true
            if (isLocalPick) {
                val part = buildImagePart(Uri.parse(cover))
                if (part == null) {
                    imageOk = false
                    toast(getString(R.string.managebook_image_read_failed))
                } else when (val up = bookRepo.uploadImage(savedBook.id, part)) {
                    is ApiResult.Success -> Unit
                    is ApiResult.Error -> {
                        imageOk = false
                        toast(up.message.ifBlank { getString(R.string.managebook_image_upload_failed) })
                    }
                }
            }

            // The book is saved regardless; only warn (above) if the cover couldn't be attached.
            if (imageOk) {
                toast(getString(if (isEdit) R.string.managebook_book_updated else R.string.managebook_book_created))
            }
            finish()
        }
    }

    /** Reads the selected image into a multipart part named `file` for the upload endpoint. */
    private fun buildImagePart(uri: Uri): MultipartBody.Part? = runCatching {
        val resolver = contentResolver
        val mime = resolver.getType(uri) ?: "image/*"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val ext = mime.substringAfterLast('/', "jpg").take(5)
        val fileName = "cover_${System.currentTimeMillis()}.$ext"
        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull(), 0, bytes.size)
        MultipartBody.Part.createFormData("file", fileName, requestBody)
    }.getOrNull()

    /** Store the chosen cover (device URI or remote URL) and reflect it in the preview. */
    private fun setImage(url: String?) {
        imageUri = url?.ifBlank { null }
        val has = imageUri != null
        if (has) {
            // Local picks load as-is; server-relative paths are resolved to an absolute URL.
            ivCoverPreview.load(ImageUrls.resolve(imageUri)) { error(R.drawable.ic_book) }
        } else {
            ivCoverPreview.setImageResource(R.drawable.ic_book)
        }
        tvRemoveImage.visibility = if (has) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"

    }
}
