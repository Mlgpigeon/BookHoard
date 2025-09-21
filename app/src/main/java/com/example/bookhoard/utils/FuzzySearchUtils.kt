package com.example.bookhoard.utils

import com.example.bookhoard.data.Book
import java.text.Normalizer
import kotlin.math.min

/**
 * Utilidades para búsqueda fuzzy (difusa) que permite encontrar coincidencias
 * incluso con erratas, acentos, y variaciones en el texto.
 */
object FuzzySearchUtils {

    /**
     * Normaliza un texto eliminando acentos, convirtiendo a minúsculas,
     * y eliminando caracteres especiales.
     */
    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Calcula la distancia de Levenshtein entre dos strings.
     * Indica cuántos cambios se necesitan para convertir una string en otra.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Calcula un score de similaridad entre 0.0 y 1.0
     * donde 1.0 es coincidencia exacta y 0.0 es completamente diferente.
     */
    private fun similarityScore(query: String, text: String): Double {
        val normalizedQuery = normalizeText(query)
        val normalizedText = normalizeText(text)

        if (normalizedQuery.isEmpty() || normalizedText.isEmpty()) return 0.0

        // Coincidencia exacta
        if (normalizedQuery == normalizedText) return 1.0

        // Contiene la query completa
        if (normalizedText.contains(normalizedQuery)) return 0.9

        // Búsqueda por tokens (palabras)
        val queryTokens = normalizedQuery.split(" ").filter { it.isNotEmpty() }
        val textTokens = normalizedText.split(" ").filter { it.isNotEmpty() }

        var tokenMatches = 0
        var partialMatches = 0

        for (queryToken in queryTokens) {
            var bestMatch = 0.0

            for (textToken in textTokens) {
                val score = when {
                    // Coincidencia exacta de token
                    queryToken == textToken -> 1.0
                    // Token contiene la query
                    textToken.contains(queryToken) -> 0.8
                    // Query contiene el token (para búsquedas parciales)
                    queryToken.contains(textToken) && textToken.length >= 3 -> 0.7
                    // Fuzzy matching con Levenshtein
                    else -> {
                        val distance = levenshteinDistance(queryToken, textToken)
                        val maxLength = maxOf(queryToken.length, textToken.length)
                        val similarity = 1.0 - (distance.toDouble() / maxLength)

                        // Solo considerar como match si la similaridad es >= 60%
                        if (similarity >= 0.6) similarity * 0.6 else 0.0
                    }
                }

                bestMatch = maxOf(bestMatch, score)
            }

            if (bestMatch >= 0.8) tokenMatches++
            else if (bestMatch >= 0.4) partialMatches++
        }

        // Calcular score final basado en matches de tokens
        return when {
            tokenMatches == queryTokens.size -> 0.85 // Todos los tokens matchean bien
            tokenMatches > 0 -> 0.7 * (tokenMatches.toDouble() / queryTokens.size)
            partialMatches > 0 -> 0.4 * (partialMatches.toDouble() / queryTokens.size)
            else -> {
                // Fallback: usar Levenshtein en el texto completo
                val distance = levenshteinDistance(normalizedQuery, normalizedText)
                val maxLength = maxOf(normalizedQuery.length, normalizedText.length)
                val similarity = 1.0 - (distance.toDouble() / maxLength)
                if (similarity >= 0.5) similarity * 0.3 else 0.0
            }
        }
    }

    /**
     * Busca libros usando fuzzy matching y los ordena por relevancia.
     */
    fun searchBooks(books: List<Book>, query: String, threshold: Double = 0.3): List<Pair<Book, Double>> {
        if (query.isBlank()) return books.map { it to 1.0 }

        val results = mutableListOf<Pair<Book, Double>>()

        for (book in books) {
            var maxScore = 0.0

            // Buscar en título (peso 40%)
            val titleScore = similarityScore(query, book.title) * 0.4
            maxScore = maxOf(maxScore, titleScore)

            // Buscar en autor (peso 30%)
            book.author?.let { author ->
                val authorScore = similarityScore(query, author) * 0.3
                maxScore = maxOf(maxScore, authorScore)
            }

            // Buscar en saga (peso 25%)
            book.saga?.let { saga ->
                val sagaScore = similarityScore(query, saga) * 0.25
                maxScore = maxOf(maxScore, sagaScore)
            }

            // Búsqueda combinada (peso 5% extra)
            val combinedText = listOfNotNull(book.title, book.author, book.saga).joinToString(" ")
            val combinedScore = similarityScore(query, combinedText) * 0.05
            maxScore += combinedScore

            if (maxScore >= threshold) {
                results.add(book to maxScore)
            }
        }

        // Ordenar por score descendente
        return results.sortedByDescending { it.second }
    }

    /**
     * Busca libros y devuelve solo la lista de libros ordenada por relevancia.
     */
    fun searchBooksSimple(books: List<Book>, query: String, threshold: Double = 0.3): List<Book> {
        return searchBooks(books, query, threshold).map { it.first }
    }

    /**
     * Busca autores usando fuzzy matching y los ordena por relevancia.
     */
    fun searchAuthors(authors: List<String>, query: String, threshold: Double = 0.4): List<Pair<String, Double>> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<Pair<String, Double>>()

        for (author in authors) {
            val score = similarityScore(query, author)
            if (score >= threshold) {
                results.add(author to score)
            }
        }

        // Ordenar por score descendente y tomar los primeros 5
        return results.sortedByDescending { it.second }.take(5)
    }

    /**
     * Busca autores y devuelve solo la lista de autores ordenada por relevancia.
     */
    fun searchAuthorsSimple(authors: List<String>, query: String, threshold: Double = 0.4): List<String> {
        return searchAuthors(authors, query, threshold).map { it.first }
    }

    /**
     * Genera sugerencias de búsqueda basadas en los libros disponibles.
     */
    fun generateSearchSuggestions(books: List<Book>, query: String, maxSuggestions: Int = 5): List<String> {
        if (query.length < 2) return emptyList()

        val suggestions = mutableSetOf<String>()

        // Recopilar todos los términos únicos
        val allTerms = mutableSetOf<String>()
        books.forEach { book ->
            allTerms.add(book.title)
            book.author?.let { allTerms.add(it) }
            book.saga?.let { allTerms.add(it) }
        }

        // Buscar términos similares
        allTerms.forEach { term ->
            val score = similarityScore(query, term)
            if (score >= 0.4) {
                suggestions.add(term)
            }
        }

        return suggestions.take(maxSuggestions).toList()
    }
}