package com.example.mybookhoard.repositories

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.data.dao.BookDao
import java.util.Date

/**
 * Repository for Book operations
 * Ready for future API integration while handling local database
 */
class BookRepository private constructor(context: Context) {

    private val bookDao: BookDao = AppDb.get(context).bookDao()
    private val authorRepository: AuthorRepository = AuthorRepository.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: BookRepository? = null

        fun getInstance(context: Context): BookRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookRepository(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    // Read operations
    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    fun getBookById(id: Long): Flow<Book?> = bookDao.getBookById(id)

    fun searchBooks(query: String): Flow<List<Book>> =
        bookDao.searchBooks("%$query%")

    fun getBooksByAuthor(authorId: Long): Flow<List<Book>> =
        bookDao.getBooksByAuthor(authorId)

    fun getBooksBySaga(sagaId: Long): Flow<List<Book>> =
        bookDao.getBooksBySaga(sagaId)

    fun getPublicBooks(): Flow<List<Book>> = bookDao.getPublicBooks()

    fun getBooksBySource(source: BookSource): Flow<List<Book>> =
        bookDao.getBooksBySource(source.name)

    fun getUniqueLanguages(): Flow<List<String>> = bookDao.getUniqueLanguages()

    // Write operations
    suspend fun addBook(book: Book): Long = withContext(Dispatchers.IO) {
        val updatedBook = book.copy(
            createdAt = if (book.id == 0L) Date() else book.createdAt,
            updatedAt = Date()
        )
        bookDao.upsert(updatedBook)
    }

    suspend fun addBooks(books: List<Book>) = withContext(Dispatchers.IO) {
        val now = Date()
        val updatedBooks = books.map { book ->
            book.copy(
                createdAt = if (book.id == 0L) now else book.createdAt,
                updatedAt = now
            )
        }
        bookDao.upsertAll(updatedBooks)
    }

    suspend fun updateBook(book: Book) = withContext(Dispatchers.IO) {
        bookDao.update(book.copy(updatedAt = Date()))
    }

    suspend fun deleteBook(book: Book) = withContext(Dispatchers.IO) {
        bookDao.delete(book)
    }

    suspend fun deleteBookById(id: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteById(id)
    }

    // Utility operations
    suspend fun getBookByIdSync(id: Long): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookByIdSync(id)
    }

    suspend fun getBookByISBN(isbn: String): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookByISBN(isbn)
    }

    suspend fun getBookCount(): Int = withContext(Dispatchers.IO) {
        bookDao.count()
    }

    suspend fun getPublicBookCount(): Int = withContext(Dispatchers.IO) {
        bookDao.countPublicBooks()
    }

    suspend fun clearAllBooks() = withContext(Dispatchers.IO) {
        bookDao.clear()
    }

    // Enhanced operations for API integration
    suspend fun addBookWithAuthor(
        title: String,
        authorName: String,
        description: String? = null,
        genres: List<String>? = null,
        publicationYear: Int? = null,
        isbn: String? = null
    ): Long = withContext(Dispatchers.IO) {
        // Find or create author
        val author = authorRepository.findOrCreateAuthorByName(authorName)

        val book = Book(
            title = title,
            description = description,
            primaryAuthorId = author.id,
            genres = genres,
            publicationYear = publicationYear,
            isbn = isbn,
            source = BookSource.USER_DEFINED
        )

        addBook(book)
    }
}