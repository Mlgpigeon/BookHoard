package com.example.mybookhoard.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.mybookhoard.data.entities.Author

@Dao
interface AuthorDao {

    @Query("SELECT * FROM authors ORDER BY name COLLATE NOCASE")
    fun getAllAuthors(): Flow<List<Author>>

    @Query("SELECT * FROM authors WHERE id = :id")
    fun getAuthorById(id: Long): Flow<Author?>

    @Query("SELECT * FROM authors WHERE id = :id")
    suspend fun getAuthorByIdSync(id: Long): Author?

    @Query("""
        SELECT * FROM authors 
        WHERE name LIKE :query 
           OR nationality LIKE :query 
        ORDER BY name COLLATE NOCASE
    """)
    fun searchAuthors(query: String): Flow<List<Author>>

    @Query("SELECT DISTINCT nationality FROM authors WHERE nationality IS NOT NULL ORDER BY nationality")
    fun getUniqueNationalities(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(author: Author): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(authors: List<Author>)

    @Update
    suspend fun update(author: Author)

    @Delete
    suspend fun delete(author: Author)

    @Query("DELETE FROM authors WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM authors")
    suspend fun count(): Int

    @Query("DELETE FROM authors")
    suspend fun clear()
}