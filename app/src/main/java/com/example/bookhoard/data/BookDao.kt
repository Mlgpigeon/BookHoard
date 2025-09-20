package com.example.bookhoard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE")
    fun all(): Flow<List<Book>>

    @Query("""
        SELECT * FROM books 
        WHERE title LIKE :query 
           OR author LIKE :query 
           OR saga LIKE :query 
        ORDER BY title COLLATE NOCASE
    """)
    fun searchBooks(query: String): Flow<List<Book>>

    @Query("""
        SELECT * FROM books 
        WHERE wishlist IS NOT NULL 
          AND (title LIKE :query OR author LIKE :query OR saga LIKE :query)
        ORDER BY title COLLATE NOCASE
    """)
    fun searchWishlistBooks(query: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY title COLLATE NOCASE")
    fun getBooksByStatus(status: ReadingStatus): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE wishlist = :wishlistStatus ORDER BY title COLLATE NOCASE")
    fun getBooksByWishlistStatus(wishlistStatus: WishlistStatus): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE author = :author ORDER BY title COLLATE NOCASE")
    fun getBooksByAuthor(author: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE saga = :saga ORDER BY title COLLATE NOCASE")
    fun getBooksBySaga(saga: String): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Book>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    suspend fun countByStatus(status: ReadingStatus): Int

    @Query("SELECT COUNT(*) FROM books WHERE wishlist = :wishlistStatus")
    suspend fun countByWishlistStatus(wishlistStatus: WishlistStatus): Int
}