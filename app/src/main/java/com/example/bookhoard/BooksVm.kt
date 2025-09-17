package com.example.bookhoard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookhoard.data.*
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BooksVm(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).bookDao()

    val items: Flow<List<Book>> = dao.all()

    fun replaceAll(list: List<Book>) {
        viewModelScope.launch {
            dao.clear()
            dao.upsertAll(list)
        }
    }

    /** Importa el CSV de assets solo si la DB está vacía */
    fun importFromAssetsOnce(ctx: Context) {
        viewModelScope.launch {
            val current = dao.all().firstOrNull() ?: emptyList()
            if (current.isEmpty()) {
                val csv = ctx.assets.open("libros_iniciales.csv")
                    .bufferedReader().use { it.readText() }
                val books = parseCsv(csv)
                dao.upsertAll(books)
            }
        }
    }

    private fun parseCsv(csv: String): List<Book> {
        val reader = csvReader {
            skipEmptyLine = true
            autoRenameDuplicateHeaders = true
        }
        val rows = reader.readAllWithHeader(csv.byteInputStream())

        return rows.mapNotNull { r ->
            val title = r["Title"]?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val readStr = r["Read"]?.trim()?.lowercase().orEmpty()
            val read = readStr in listOf("true","1","sí","si","x","✓","✔")
            val saga = r["Saga"]?.trim().orEmpty().ifBlank { null }
            val author = r["Author"]?.trim().orEmpty().ifBlank { null }
            val status = when (readStr) {
                "leyendo" -> ReadingStatus.READING
                "true","1","sí","si","x","✓","✔" -> ReadingStatus.READ
                else -> ReadingStatus.NOT_STARTED
            }
            Book(title = title, author = author, saga = saga, status = status)
        }
    }
    fun updateStatus(book: Book, status: ReadingStatus) {
        viewModelScope.launch {
            val updated = book.copy(status = status)
            dao.upsertAll(listOf(updated))
        }
    }

    fun updateWishlist(book: Book, status: WishlistStatus?) {
        viewModelScope.launch {
            val updated = book.copy(wishlist = status)
            dao.upsertAll(listOf(updated))
        }
    }

}
