package com.example.mybookhoard.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.data.entities.BookWithUserData

@Dao
interface UserBookDao {

    @Query("SELECT * FROM user_books WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getUserBooks(userId: Long): Flow<List<UserBook>>

    @Query("SELECT * FROM user_books WHERE user_id = :userId AND book_id = :bookId")
    fun getUserBook(userId: Long, bookId: Long): Flow<UserBook?>

    @Query("SELECT * FROM user_books WHERE user_id = :userId AND book_id = :bookId")
    suspend fun getUserBookSync(userId: Long, bookId: Long): UserBook?

    @Query("""
        SELECT * FROM user_books 
        WHERE user_id = :userId 
          AND reading_status = :status 
        ORDER BY updated_at DESC
    """)
    fun getUserBooksByStatus(userId: Long, status: UserBookReadingStatus): Flow<List<UserBook>>

    @Query("""
        SELECT * FROM user_books 
        WHERE user_id = :userId 
          AND wishlist_status = :status 
        ORDER BY updated_at DESC
    """)
    fun getUserBooksByWishlist(userId: Long, status: UserBookWishlistStatus): Flow<List<UserBook>>

    @Query("""
        SELECT * FROM user_books 
        WHERE user_id = :userId 
          AND favorite = 1 
        ORDER BY updated_at DESC
    """)
    fun getFavoriteBooks(userId: Long): Flow<List<UserBook>>

    @Query("""
        SELECT b.*, ub.* FROM books b
        LEFT JOIN user_books ub ON b.id = ub.book_id AND ub.user_id = :userId
        WHERE b.is_public = 1 OR ub.id IS NOT NULL
        ORDER BY b.title COLLATE NOCASE
    """)
    fun getBooksWithUserData(userId: Long): Flow<List<BookWithUserData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(userBook: UserBook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(userBooks: List<UserBook>)

    @Update
    suspend fun update(userBook: UserBook)

    @Delete
    suspend fun delete(userBook: UserBook)

    @Query("DELETE FROM user_books WHERE user_id = :userId AND book_id = :bookId")
    suspend fun deleteUserBook(userId: Long, bookId: Long)

    @Query("DELETE FROM user_books WHERE user_id = :userId")
    suspend fun deleteAllUserBooks(userId: Long)

    @Query("SELECT COUNT(*) FROM user_books WHERE user_id = :userId")
    suspend fun countUserBooks(userId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM user_books 
        WHERE user_id = :userId AND reading_status = :status
    """)
    suspend fun countByStatus(userId: Long, status: UserBookReadingStatus): Int

    @Query("DELETE FROM user_books")
    suspend fun clear()
}