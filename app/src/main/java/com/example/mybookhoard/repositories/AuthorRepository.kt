package com.example.mybookhoard.repositories

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.mybookhoard.data.AppDb
import com.example.mybookhoard.data.entities.Author
import com.example.mybookhoard.data.dao.AuthorDao

/**
 * Repository for Author operations
 * Handles local database and future API sync integration
 */
class AuthorRepository private constructor(context: Context) {

    private val authorDao: AuthorDao = AppDb.get(context).authorDao()

    companion object {
        @Volatile
        private var INSTANCE: AuthorRepository? = null

        fun getInstance(context: Context): AuthorRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthorRepository(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    // Read operations
    fun getAllAuthors(): Flow<List<Author>> = authorDao.getAllAuthors()

    fun getAuthorById(id: Long): Flow<Author?> = authorDao.getAuthorById(id)

    fun searchAuthors(query: String): Flow<List<Author>> =
        authorDao.searchAuthors("%$query%")

    fun getUniqueNationalities(): Flow<List<String>> = authorDao.getUniqueNationalities()

    // Write operations
    suspend fun addAuthor(author: Author): Long = withContext(Dispatchers.IO) {
        authorDao.upsert(author.copy(updatedAt = java.util.Date()))
    }

    suspend fun addAuthors(authors: List<Author>) = withContext(Dispatchers.IO) {
        val now = java.util.Date()
        val updatedAuthors = authors.map { it.copy(updatedAt = now) }
        authorDao.upsertAll(updatedAuthors)
    }

    suspend fun updateAuthor(author: Author) = withContext(Dispatchers.IO) {
        authorDao.update(author.copy(updatedAt = java.util.Date()))
    }

    suspend fun deleteAuthor(author: Author) = withContext(Dispatchers.IO) {
        authorDao.delete(author)
    }

    suspend fun deleteAuthorById(id: Long) = withContext(Dispatchers.IO) {
        authorDao.deleteById(id)
    }

    // Utility operations
    suspend fun getAuthorByIdSync(id: Long): Author? = withContext(Dispatchers.IO) {
        authorDao.getAuthorByIdSync(id)
    }

    suspend fun getAuthorCount(): Int = withContext(Dispatchers.IO) {
        authorDao.count()
    }

    suspend fun clearAllAuthors() = withContext(Dispatchers.IO) {
        authorDao.clear()
    }

    // Helper method to find or create author by name
    suspend fun findOrCreateAuthorByName(authorName: String): Author = withContext(Dispatchers.IO) {
        // For now, just create a new author
        // TODO: Implement search and fuzzy matching in the future
        val author = Author(name = authorName)
        val authorId = authorDao.upsert(author)
        author.copy(id = authorId)
    }
}