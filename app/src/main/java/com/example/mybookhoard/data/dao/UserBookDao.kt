package com.example.mybookhoard.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.data.entities.BookWithUserData
import com.example.mybookhoard.data.entities.BookUserResult

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
        SELECT 
            b.id, b.title, b.original_title, b.description, b.primary_author_id, 
            b.saga_id, b.saga_number, b.language, b.publication_year, b.genres,
            b.isbn, b.cover_selected, b.images, b.adaptations, b.average_rating,
            b.total_ratings, b.is_public, b.source, b.created_at, b.updated_at,
            ub.id as ub_id, ub.user_id as ub_user_id, ub.reading_status as ub_reading_status,
            ub.wishlist_status as ub_wishlist_status, ub.personal_rating as ub_personal_rating,
            ub.review as ub_review, ub.annotations as ub_annotations, ub.reading_progress as ub_reading_progress,
            ub.date_started as ub_date_started, ub.date_finished as ub_date_finished,
            ub.favorite as ub_favorite, ub.created_at as ub_created_at, ub.updated_at as ub_updated_at
        FROM books b
        LEFT JOIN user_books ub ON b.id = ub.book_id AND ub.user_id = :userId
        WHERE b.is_public = 1 OR ub.id IS NOT NULL
        ORDER BY b.title COLLATE NOCASE
    """)
    fun getBooksWithUserDataRaw(userId: Long): Flow<List<BookUserResult>>

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