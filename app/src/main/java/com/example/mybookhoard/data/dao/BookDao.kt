package com.example.mybookhoard.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.mybookhoard.data.entities.Book

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): Book?

    @Query("""
        SELECT * FROM books 
        WHERE title LIKE :query 
           OR description LIKE :query 
        ORDER BY title COLLATE NOCASE
    """)
    fun searchBooks(query: String): Flow<List<Book>>


    @Query("""
    SELECT b.* FROM books b
    LEFT JOIN authors a ON b.primary_author_id = a.id
    WHERE b.is_public = 1 
      AND (b.title LIKE :query 
           OR b.description LIKE :query 
           OR a.name LIKE :query)
    ORDER BY b.title COLLATE NOCASE
    """)
    fun searchPublicBooks(query: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE primary_author_id = :authorId ORDER BY title COLLATE NOCASE")
    fun getBooksByAuthor(authorId: Long): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE saga_id = :sagaId ORDER BY saga_number ASC, title COLLATE NOCASE")
    fun getBooksBySaga(sagaId: Long): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isbn = :isbn")
    suspend fun getBookByISBN(isbn: String): Book?

    @Query("SELECT * FROM books WHERE is_public = 1 ORDER BY title COLLATE NOCASE")
    fun getPublicBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE source = :source ORDER BY title COLLATE NOCASE")
    fun getBooksBySource(source: String): Flow<List<Book>>

    @Query("SELECT DISTINCT language FROM books WHERE language IS NOT NULL ORDER BY language")
    fun getUniqueLanguages(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: Book): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<Book>)

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM books WHERE is_public = 1")
    suspend fun countPublicBooks(): Int

    @Query("DELETE FROM books")
    suspend fun clear()
}