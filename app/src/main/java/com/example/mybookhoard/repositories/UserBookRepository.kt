package com.example.mybookhoard.repositories

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.mybookhoard.data.AppDb
import com.example.mybookhoard.data.*
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.data.dao.UserBookDao
import java.util.Date

/**
 * Repository for UserBook operations
 * Handles user-book relationships and reading tracking
 */
class UserBookRepository private constructor(context: Context) {

    private val userBookDao: UserBookDao = AppDb.get(context).userBookDao()

    companion object {
        @Volatile
        private var INSTANCE: UserBookRepository? = null

        fun getInstance(context: Context): UserBookRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserBookRepository(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    // Read operations
    fun getUserBooks(userId: Long): Flow<List<UserBook>> =
        userBookDao.getUserBooks(userId)

    fun getUserBook(userId: Long, bookId: Long): Flow<UserBook?> =
        userBookDao.getUserBook(userId, bookId)

    fun getUserBooksByStatus(userId: Long, status: UserBookReadingStatus): Flow<List<UserBook>> =
        userBookDao.getUserBooksByStatus(userId, status)

    fun getUserBooksByWishlist(userId: Long, status: UserBookWishlistStatus): Flow<List<UserBook>> =
        userBookDao.getUserBooksByWishlist(userId, status)

    fun getFavoriteBooks(userId: Long): Flow<List<UserBook>> =
        userBookDao.getFavoriteBooks(userId)

    fun getBooksWithUserData(userId: Long): Flow<List<BookWithUserData>> =
        userBookDao.getBooksWithUserDataRaw(userId).map { resultList ->
            resultList.map { it.toBookWithUserData() }
        }

    fun getBooksWithUserDataExtended(userId: Long): Flow<List<BookWithUserDataExtended>> =
        userBookDao.getBooksWithUserDataRaw(userId).map { resultList ->
            resultList.map { it.toBookWithUserDataExtended() }
        }

    // Write operations
    suspend fun addUserBook(userBook: UserBook): Long = withContext(Dispatchers.IO) {
        userBookDao.upsert(userBook.copy(updatedAt = Date()))
    }

    suspend fun addUserBooks(userBooks: List<UserBook>) = withContext(Dispatchers.IO) {
        val updatedBooks = userBooks.map { it.copy(updatedAt = Date()) }
        userBookDao.upsertAll(updatedBooks)
    }

    suspend fun updateUserBook(userBook: UserBook) = withContext(Dispatchers.IO) {
        userBookDao.update(userBook.copy(updatedAt = Date()))
    }

    suspend fun deleteUserBook(userBook: UserBook) = withContext(Dispatchers.IO) {
        userBookDao.delete(userBook)
    }

    suspend fun deleteUserBook(userId: Long, bookId: Long) = withContext(Dispatchers.IO) {
        userBookDao.deleteUserBook(userId, bookId)
    }

    suspend fun deleteAllUserBooks(userId: Long) = withContext(Dispatchers.IO) {
        userBookDao.deleteAllUserBooks(userId)
    }

    // Utility operations
    suspend fun getUserBookSync(userId: Long, bookId: Long): UserBook? = withContext(Dispatchers.IO) {
        userBookDao.getUserBookSync(userId, bookId)
    }

    suspend fun getUserBookCount(userId: Long): Int = withContext(Dispatchers.IO) {
        userBookDao.countUserBooks(userId)
    }

    suspend fun getCountByStatus(userId: Long, status: UserBookReadingStatus): Int = withContext(Dispatchers.IO) {
        userBookDao.countByStatus(userId, status)
    }

    suspend fun clearAllUserBooks() = withContext(Dispatchers.IO) {
        userBookDao.clear()
    }

    // Convenience methods for common reading operations
    suspend fun markAsReading(
        userId: Long,
        bookId: Long,
        dateStarted: Date = Date()
    ) = withContext(Dispatchers.IO) {
        val existingUserBook = userBookDao.getUserBookSync(userId, bookId)
        val userBook = existingUserBook?.copy(
            readingStatus = UserBookReadingStatus.READING,
            dateStarted = dateStarted,
            updatedAt = Date()
        ) ?: UserBook(
            userId = userId,
            bookId = bookId,
            readingStatus = UserBookReadingStatus.READING,
            dateStarted = dateStarted
        )
        userBookDao.upsert(userBook)
    }

    suspend fun markAsRead(
        userId: Long,
        bookId: Long,
        dateFinished: Date = Date(),
        rating: Float? = null
    ) = withContext(Dispatchers.IO) {
        val existingUserBook = userBookDao.getUserBookSync(userId, bookId)
        val userBook = existingUserBook?.copy(
            readingStatus = UserBookReadingStatus.READ,
            dateFinished = dateFinished,
            personalRating = rating,
            readingProgress = 100,
            updatedAt = Date()
        ) ?: UserBook(
            userId = userId,
            bookId = bookId,
            readingStatus = UserBookReadingStatus.READ,
            dateFinished = dateFinished,
            personalRating = rating,
            readingProgress = 100
        )
        userBookDao.upsert(userBook)
    }

    suspend fun toggleFavorite(userId: Long, bookId: Long) = withContext(Dispatchers.IO) {
        val existingUserBook = userBookDao.getUserBookSync(userId, bookId)
        if (existingUserBook != null) {
            val updatedUserBook = existingUserBook.copy(
                favorite = !existingUserBook.favorite,
                updatedAt = Date()
            )
            userBookDao.update(updatedUserBook)
        }
    }
}