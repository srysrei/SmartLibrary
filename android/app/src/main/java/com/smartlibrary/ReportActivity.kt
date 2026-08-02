package com.smartlibrary

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.smartlibrary.data.repository.BookRepository
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.BorrowResponse
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin-only screen that surfaces library-wide totals and lets the admin export the
 * catalog, currently-borrowed items and completed returns as CSV or PDF files for sharing.
 */
class ReportActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var bookRepo: BookRepository
    private lateinit var borrowRepo: BorrowRepository

    /** One export's data, independent of the file format it ends up written as. */
    private data class ReportData(
        val title: String,
        val baseName: String,
        val header: List<String>,
        val rows: List<List<String>>,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        session = SessionManager(this)
        bookRepo = BookRepository(this)
        borrowRepo = BorrowRepository(this)

        val onAvatar = View.OnClickListener {
            startActivity(Intent(this@ReportActivity, ProfileActivity::class.java))
        }
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        tvAvatar.setOnClickListener(onAvatar)
        ivAvatar.setOnClickListener(onAvatar)
        UiHelpers.bindAvatar(tvAvatar, ivAvatar, session.imageUrl, session.initials())

        findViewById<MaterialButton>(R.id.btnExportBooksCsv).setOnClickListener { runExport(::booksReport, isPdf = false) }
        findViewById<MaterialButton>(R.id.btnExportBooksPdf).setOnClickListener { runExport(::booksReport, isPdf = true) }
        findViewById<MaterialButton>(R.id.btnExportBorrowedCsv).setOnClickListener { runExport(::borrowedReport, isPdf = false) }
        findViewById<MaterialButton>(R.id.btnExportBorrowedPdf).setOnClickListener { runExport(::borrowedReport, isPdf = true) }
        findViewById<MaterialButton>(R.id.btnExportReturnedCsv).setOnClickListener { runExport(::returnedReport, isPdf = false) }
        findViewById<MaterialButton>(R.id.btnExportReturnedPdf).setOnClickListener { runExport(::returnedReport, isPdf = true) }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.inflateMenu(R.menu.menu_admin_nav)
        NavHelper.setupAdmin(this, nav, R.id.nav_reports)
    }

    override fun onResume() {
        super.onResume()
        loadSummary()
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            when (val r = bookRepo.fetchBooks(null, null)) {
                is ApiResult.Success -> findViewById<TextView>(R.id.tvTotalBooks).text = r.data.size.toString()
                is ApiResult.Error -> {}
            }
        }
        lifecycleScope.launch {
            when (val r = borrowRepo.allBorrows(null)) {
                is ApiResult.Success -> {
                    val borrowedCount = r.data.filter { it.isActiveLoan() }.sumOf { b -> b.items.count { !it.returned } }
                    val returnedCount = r.data.sumOf { b -> b.items.count { it.returned } }
                    findViewById<TextView>(R.id.tvBorrowedCount).text = borrowedCount.toString()
                    findViewById<TextView>(R.id.tvReturnedCount).text = returnedCount.toString()
                }
                is ApiResult.Error -> {}
            }
        }
    }

    private suspend fun booksReport(): ReportData? = when (val r = bookRepo.fetchBooks(null, null)) {
        is ApiResult.Success -> ReportData(
            title = getString(R.string.reports_export_books_title),
            baseName = "all_books",
            header = listOf("ID", "Title", "Author", "Category", "Quantity", "Available"),
            rows = r.data.map { b ->
                listOf(
                    b.id.toString(), b.title, b.author, b.categoryName ?: "",
                    b.qty.toString(), b.availableQty.toString(),
                )
            },
        )
        is ApiResult.Error -> { toast(r.message); null }
    }

    private suspend fun borrowedReport(): ReportData? = when (val r = borrowRepo.allBorrows(null)) {
        is ApiResult.Success -> ReportData(
            title = getString(R.string.reports_export_borrowed_title),
            baseName = "borrowed_books",
            header = listOf("Borrow ID", "Title", "Author", "Borrower", "Borrow Date", "Days", "Status"),
            rows = r.data.filter { it.isActiveLoan() }.flatMap { b ->
                b.items.filter { !it.returned }.map { item ->
                    listOf(
                        b.id.toString(), item.title ?: "", item.author ?: "",
                        b.userName ?: "", b.borrowDate ?: "", b.borrowDay.toString(), b.status,
                    )
                }
            },
        )
        is ApiResult.Error -> { toast(r.message); null }
    }

    /** A borrow currently has books out on loan only once it's approved and not fully returned. */
    private fun BorrowResponse.isActiveLoan(): Boolean =
        status.equals("APPROVED", true) || status.equals("RETURN", true)

    private suspend fun returnedReport(): ReportData? = when (val r = borrowRepo.allBorrows(null)) {
        is ApiResult.Success -> ReportData(
            title = getString(R.string.reports_export_returned_title),
            baseName = "returned_books",
            header = listOf("Borrow ID", "Title", "Author", "Borrower", "Borrow Date", "Return Date"),
            rows = r.data.flatMap { b ->
                b.items.filter { it.returned }.map { item ->
                    listOf(
                        b.id.toString(), item.title ?: "", item.author ?: "",
                        b.userName ?: "", b.borrowDate ?: "", item.returnDate ?: "",
                    )
                }
            },
        )
        is ApiResult.Error -> { toast(r.message); null }
    }

    private fun runExport(loader: suspend () -> ReportData?, isPdf: Boolean) {
        lifecycleScope.launch {
            val data = loader() ?: return@launch
            if (data.rows.isEmpty()) {
                toast(getString(R.string.reports_export_empty))
                return@launch
            }
            try {
                val file = if (isPdf) writePdf(data) else writeCsv(data)
                share(file, if (isPdf) "application/pdf" else "text/csv")
            } catch (e: Exception) {
                toast(getString(R.string.reports_export_failed))
            }
        }
    }

    private fun reportsDir(): File = File(cacheDir, "reports").apply { mkdirs() }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun writeCsv(data: ReportData): File {
        val file = File(reportsDir(), "${data.baseName}_${stamp()}.csv")
        FileWriter(file).use { writer ->
            writer.append(data.header.joinToString(",") { csvEscape(it) }).append("\n")
            data.rows.forEach { row -> writer.append(row.joinToString(",") { csvEscape(it) }).append("\n") }
        }
        return file
    }

    private fun csvEscape(value: String): String =
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value

    /** Renders a simple, paginated table (title + header row + data rows) into a PDF. */
    private fun writePdf(data: ReportData): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val lineHeight = 16f

        val titlePaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.BLACK }
        val metaPaint = Paint().apply { textSize = 9.5f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 9.5f; isFakeBoldText = true; color = Color.BLACK }
        val cellPaint = Paint().apply { textSize = 9f; color = Color.DKGRAY }

        // Column x-offsets, sized proportionally to the widest content in each column.
        val usableWidth = pageWidth - margin * 2
        val colCount = data.header.size
        val colWeight = FloatArray(colCount) { i ->
            val maxLen = maxOf(data.header[i].length, data.rows.maxOfOrNull { it.getOrNull(i)?.length ?: 0 } ?: 0)
            maxLen.toFloat().coerceAtLeast(1f)
        }
        val totalWeight = colWeight.sum()
        val colX = FloatArray(colCount)
        var acc = margin
        for (i in 0 until colCount) {
            colX[i] = acc
            acc += (colWeight[i] / totalWeight) * usableWidth
        }

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 10f

        fun drawRow(values: List<String>, paint: Paint) {
            values.forEachIndexed { i, v -> canvas.drawText(v, colX[i], y, paint) }
            y += lineHeight
        }

        fun startNewPage() {
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin + 10f
            drawRow(data.header, headerPaint)
            y += 4f
        }

        canvas.drawText(data.title, margin, y, titlePaint)
        y += lineHeight * 1.4f
        canvas.drawText(
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date()),
            margin, y, metaPaint,
        )
        y += lineHeight * 1.3f
        drawRow(data.header, headerPaint)
        y += 4f

        data.rows.forEach { row ->
            if (y > pageHeight - margin) startNewPage()
            drawRow(row, cellPaint)
        }
        pdf.finishPage(page)

        val file = File(reportsDir(), "${data.baseName}_${stamp()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    private fun share(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.reports_export_share_title)))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}