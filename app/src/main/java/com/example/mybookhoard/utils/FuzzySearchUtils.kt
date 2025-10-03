package com.example.mybookhoard.utils

import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.BookWithUserData
import com.example.mybookhoard.data.entities.BookWithUserDataExtended
import kotlin.math.max
import kotlin.math.min

object FuzzySearchUtils {

    /**
     * Performs fuzzy search on books with user data
     */
    fun searchBooks(books: List<BookWithUserData>, query: String): List<BookWithUserData> {
        if (query.isBlank()) return books

        val normalizedQuery = query.lowercase().trim()

        return books
            .mapNotNull { bookWithUserData ->
                val score = calculateBookScore(bookWithUserData.book, normalizedQuery)
                if (score > 0) bookWithUserData to score else null
            }
            .sortedByDescending { it.second } // Sort by score descending
            .map { it.first }
    }

    /**
     * Generates search suggestions based on books
     */
    fun generateSuggestions(books: List<Book>, query: String): List<String> {
        if (query.length < 2) return emptyList()

        val normalizedQuery = query.lowercase().trim()
        val suggestions = mutableSetOf<String>()

        books.forEach { book ->
            // Title suggestions
            if (book.title.lowercase().contains(normalizedQuery)) {
                suggestions.add(book.title)
            }

            // Genre suggestions
            book.genres?.forEach { genre ->
                if (genre.lowercase().contains(normalizedQuery)) {
                    suggestions.add(genre)
                }
            }

            // Language suggestions
            if (book.language.lowercase().contains(normalizedQuery)) {
                suggestions.add(book.language.uppercase())
            }
        }

        return suggestions
            .filter { it.lowercase().startsWith(normalizedQuery) }
            .take(5)
            .toList()
    }

    /**
     * Calculates relevance score for a book against a search query
     */
    private fun calculateBookScore(book: Book, normalizedQuery: String): Double {
        var score = 0.0

        // Exact title match (highest weight)
        if (book.title.lowercase() == normalizedQuery) {
            score += 100.0
        } else if (book.title.lowercase().contains(normalizedQuery)) {
            // Partial title match
            score += 80.0

            // Boost if query is at the beginning of title
            if (book.title.lowercase().startsWith(normalizedQuery)) {
                score += 20.0
            }
        } else {
            // Fuzzy title match using Levenshtein distance
            val titleDistance = levenshteinDistance(
                book.title.lowercase(),
                normalizedQuery
            )
            val titleSimilarity = calculateSimilarity(book.title.lowercase(), normalizedQuery, titleDistance)
            if (titleSimilarity > 0.6) {
                score += titleSimilarity * 60.0
            }
        }

        // Description match (medium weight)
        if (!book.description.isNullOrBlank()) {
            if (book.description.lowercase().contains(normalizedQuery)) {
                score += 30.0
            }
        }

        // Genre match (medium weight)
        book.genres?.forEach { genre ->
            if (genre.lowercase().contains(normalizedQuery)) {
                score += 25.0
            }
        }

        // Language match (low weight)
        if (book.language.lowercase().contains(normalizedQuery)) {
            score += 10.0
        }

        // Publication year match (low weight)
        book.publicationYear?.toString()?.let { year ->
            if (year.contains(normalizedQuery)) {
                score += 15.0
            }
        }

        return score
    }

    /**
     * Calculates Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        // Initialize first row and column
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        // Fill the dp table
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Calculates similarity score based on Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String, distance: Int): Double {
        val maxLength = max(s1.length, s2.length)
        return if (maxLength == 0) 1.0 else 1.0 - (distance.toDouble() / maxLength)
    }

    fun searchPublicBooks(books: List<Book>, query: String): List<Book> {
        if (query.isBlank()) return books

        val normalizedQuery = query.lowercase().trim()

        return books
            .mapNotNull { book ->
                val score = calculateBookScore(book, normalizedQuery)
                if (score > 0) book to score else null
            }
            .sortedByDescending { it.second } // Sort by score descending
            .map { it.first }
    }

    fun searchBooksExtended(books: List<BookWithUserDataExtended>, query: String): List<BookWithUserDataExtended> {
        if (query.isBlank()) return books

        val normalizedQuery = query.lowercase().trim()

        return books
            .mapNotNull { bookWithUserData ->
                // Calculate score considering author and saga from extended data
                val score = calculateBookScoreExtended(
                    book = bookWithUserData.book,
                    authorName = bookWithUserData.authorName,
                    sagaName = bookWithUserData.sagaName,
                    normalizedQuery = normalizedQuery
                )
                if (score > 0) bookWithUserData to score else null
            }
            .sortedByDescending { it.second } // Sort by score descending
            .map { it.first }
    }

    /**
     * Calculates relevance score for a book with extended data (includes author and saga names)
     */
    private fun calculateBookScoreExtended(
        book: Book,
        authorName: String?,
        sagaName: String?,
        normalizedQuery: String
    ): Double {
        var score = calculateBookScore(book, normalizedQuery)

        // Author match (high weight - 70 points)
        if (!authorName.isNullOrBlank()) {
            if (authorName.lowercase() == normalizedQuery) {
                score += 70.0
            } else if (authorName.lowercase().contains(normalizedQuery)) {
                score += 60.0
                if (authorName.lowercase().startsWith(normalizedQuery)) {
                    score += 10.0
                }
            } else {
                // Fuzzy author match
                val authorDistance = levenshteinDistance(
                    authorName.lowercase(),
                    normalizedQuery
                )
                val authorSimilarity = calculateSimilarity(authorName.lowercase(), normalizedQuery, authorDistance)
                if (authorSimilarity > 0.6) {
                    score += authorSimilarity * 50.0
                }
            }
        }

        // Saga match (medium-high weight - 50 points)
        if (!sagaName.isNullOrBlank()) {
            if (sagaName.lowercase() == normalizedQuery) {
                score += 50.0
            } else if (sagaName.lowercase().contains(normalizedQuery)) {
                score += 40.0
                if (sagaName.lowercase().startsWith(normalizedQuery)) {
                    score += 10.0
                }
            } else {
                // Fuzzy saga match
                val sagaDistance = levenshteinDistance(
                    sagaName.lowercase(),
                    normalizedQuery
                )
                val sagaSimilarity = calculateSimilarity(sagaName.lowercase(), normalizedQuery, sagaDistance)
                if (sagaSimilarity > 0.6) {
                    score += sagaSimilarity * 35.0
                }
            }
        }

        return score
    }

}