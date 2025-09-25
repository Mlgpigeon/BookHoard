package com.example.mybookhoard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBookDao {

    // Basic CRUD operations
    @Query("SELECT * FROM user_books WHERE user_id = :userId ORDER BY created_at DESC")
    fun getUserBooks(userId: Long): Flow<List<UserBook>>

    @Query("SELECT * FROM user_books WHERE id = :id AND user_id = :userId")
    fun getUserBookById(id: Long, userId: Long): Flow<UserBook?>

    @Query("SELECT * FROM user_books WHERE user_id = :userId AND book_id = :bookId")
    fun getUserBookByBookId(userId: Long, bookId: Long): Flow<UserBook?>

    // Status-based queries
    @Query("SELECT * FROM user_books WHERE user_id = :userId AND reading_status = :status ORDER BY updated_at DESC")
    fun getUserBooksByStatus(userId: Long, status: UserBookReadingStatus): Flow<List<UserBook>>

    @Query("SELECT * FROM user_books WHERE user_id = :userId AND wishlist_status = :status ORDER BY created_at DESC")
    fun getUserBooksByWishlistStatus(userId: Long, status: UserBookWishlistStatus): Flow<List<UserBook>>

    // Favorites
    @Query("SELECT * FROM user_books WHERE user_id = :userId AND favorite = 1 ORDER BY updated_at DESC")
    fun getFavoriteBooks(userId: Long): Flow<List<UserBook>>

    // Reading progress queries
    @Query("SELECT * FROM user_books WHERE user_id = :userId AND reading_status = 'READING' ORDER BY reading_progress DESC")
    fun getCurrentlyReading(userId: Long): Flow<List<UserBook>>

    @Query("SELECT * FROM user_books WHERE user_id = :userId AND personal_rating IS NOT NULL ORDER BY personal_rating DESC, updated_at DESC")
    fun getRatedBooks(userId: Long): Flow<List<UserBook>>

    // Search functionality
    @Query("""
        SELECT ub.* FROM user_books ub 
        INNER JOIN books b ON b.id = ub.book_id 
        WHERE ub.user_id = :userId 
        AND (b.title LIKE :query OR b.author LIKE :query OR b.saga LIKE :query OR ub.review LIKE :query)
        ORDER BY b.title COLLATE NOCASE
    """)
    fun searchUserBooks(userId: Long, query: String): Flow<List<UserBook>>

    // Statistics queries
    @Query("SELECT COUNT(*) FROM user_books WHERE user_id = :userId")
    suspend fun getTotalBooksCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM user_books WHERE user_id = :userId AND reading_status = :status")
    suspend fun getCountByStatus(userId: Long, status: UserBookReadingStatus): Int

    @Query("SELECT COUNT(*) FROM user_books WHERE user_id = :userId AND wishlist_status = :status")
    suspend fun getCountByWishlistStatus(userId: Long, status: UserBookWishlistStatus): Int

    @Query("SELECT AVG(personal_rating) FROM user_books WHERE user_id = :userId AND personal_rating IS NOT NULL")
    suspend fun getAverageRating(userId: Long): Float?

    // Insert/Update/Delete operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userBook: UserBook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(userBooks: List<UserBook>)

    @Update
    suspend fun update(userBook: UserBook)

    @Delete
    suspend fun delete(userBook: UserBook)

    @Query("DELETE FROM user_books WHERE user_id = :userId")
    suspend fun deleteAllUserBooks(userId: Long)

    @Query("DELETE FROM user_books WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(id: Long, userId: Long)

    // Utility methods for sync
    @Query("SELECT * FROM user_books WHERE user_id = :userId AND updated_at > :lastSync")
    suspend fun getUpdatedSince(userId: Long, lastSync: Long): List<UserBook>

    @Query("UPDATE user_books SET updated_at = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long)
}