package com.example.mybookhoard.repositories

import android.content.Context
import com.example.mybookhoard.data.*
import com.example.mybookhoard.utils.FuzzySearchUtils
import kotlinx.coroutines.flow.*

/**
 * Handles all book search operations including local fuzzy search
 */
class BookSearchService(
    private val context: Context,
    private val localDao: BookDao
) {
    companion object {
        private const val TAG = "BookSearchService"
        private const val DEFAULT_FUZZY_THRESHOLD = 0.25
    }

    // Basic search operations
    fun searchBooks(query: String): Flow<List<Book>> {
        return if (query.isBlank()) {
            localDao.all()
        } else {
            localDao.all().map { books ->
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = DEFAULT_FUZZY_THRESHOLD)
            }
        }
    }

    fun searchWishlistBooks(query: String): Flow<List<Book>> {
        return getWishlistBooks().map { books ->
            if (query.isBlank()) {
                books
            } else {
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = DEFAULT_FUZZY_THRESHOLD)
            }
        }
    }

    // Filter operations
    fun getBooksByStatus(status: ReadingStatus): Flow<List<Book>> {
        return localDao.getBooksByStatus(status)
    }

    fun searchBooksByStatus(status: ReadingStatus, query: String): Flow<List<Book>> {
        return if (query.isBlank()) {
            getBooksByStatus(status)
        } else {
            getBooksByStatus(status).map { books ->
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = DEFAULT_FUZZY_THRESHOLD)
            }
        }
    }

    fun getBooksByAuthor(author: String): Flow<List<Book>> {
        return localDao.getBooksByAuthor(author)
    }

    fun searchBooksByAuthor(author: String, query: String): Flow<List<Book>> {
        return if (query.isBlank()) {
            getBooksByAuthor(author)
        } else {
            getBooksByAuthor(author).map { books ->
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = DEFAULT_FUZZY_THRESHOLD)
            }
        }
    }

    fun getBooksBySaga(saga: String): Flow<List<Book>> {
        return localDao.getBooksBySaga(saga)
    }

    fun searchBooksBySaga(saga: String, query: String): Flow<List<Book>> {
        return if (query.isBlank()) {
            getBooksBySaga(saga)
        } else {
            getBooksBySaga(saga).map { books ->
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = DEFAULT_FUZZY_THRESHOLD)
            }
        }
    }

    // Wishlist operations
    fun getWishlistBooks(): Flow<List<Book>> {
        return localDao.all().map { books ->
            books.filter {
                it.wishlist == WishlistStatus.WISH || it.wishlist == WishlistStatus.ON_THE_WAY
            }
        }
    }

    fun getBooksByWishlistStatus(wishlistStatus: WishlistStatus): Flow<List<Book>> {
        return localDao.getBooksByWishlistStatus(wishlistStatus)
    }

    // Metadata operations
    fun getUniqueAuthors(): Flow<List<String>> {
        return localDao.getUniqueAuthors()
    }

    fun getUniqueSagas(): Flow<List<String>> {
        return localDao.getUniqueSagas()
    }

    // Statistics
    suspend fun getTotalBooksCount(): Int {
        return localDao.count()
    }

    suspend fun getCountByStatus(status: ReadingStatus): Int {
        return localDao.countByStatus(status)
    }

    suspend fun getCountByWishlistStatus(wishlistStatus: WishlistStatus): Int {
        return localDao.countByWishlistStatus(wishlistStatus)
    }

    // Advanced search with custom threshold
    fun searchBooksWithThreshold(query: String, threshold: Double): Flow<List<Book>> {
        return if (query.isBlank()) {
            localDao.all()
        } else {
            localDao.all().map { books ->
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = threshold)
            }
        }
    }

    // Get single book
    fun getBookById(id: Long): Flow<Book?> {
        return localDao.getBookById(id)
    }

    // Combined filters
    fun getBooksWithMultipleFilters(
        status: ReadingStatus? = null,
        author: String? = null,
        saga: String? = null,
        wishlistStatus: WishlistStatus? = null,
        query: String = ""
    ): Flow<List<Book>> {
        return localDao.all().map { books ->
            var filteredBooks = books

            // Apply status filter
            status?.let { statusFilter ->
                filteredBooks = filteredBooks.filter { it.status == statusFilter }
            }

            // Apply author filter
            author?.let { authorFilter ->
                filteredBooks = filteredBooks.filter { it.author == authorFilter }
            }

            // Apply saga filter
            saga?.let { sagaFilter ->
                filteredBooks = filteredBooks.filter { it.saga == sagaFilter }
            }

            // Apply wishlist filter
            wishlistStatus?.let { wishlistFilter ->
                filteredBooks = filteredBooks.filter { it.wishlist == wishlistFilter }
            }

            // Apply text search
            if (query.isNotBlank()) {
                filteredBooks = FuzzySearchUtils.searchBooksSimple(
                    filteredBooks,
                    query,
                    threshold = DEFAULT_FUZZY_THRESHOLD
                )
            }

            filteredBooks
        }
    }
}