package com.example.mybookhoard.data.entities

/**
 * Extended version of BookWithUserData that includes additional API information
 * like resolved author name from API responses
 */
data class BookWithUserDataExtended(
    val book: Book,
    val userBook: UserBook?,
    val authorName: String? = null,
    val sagaName: String? = null,
    val sourceLabel: String? = null
)