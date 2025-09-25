package com.example.mybookhoard.data.entities

/**
 * Combined data for showing books with user info
 * Simple data class that doesn't require Room annotations
 */
data class BookWithUserData(
    val book: Book,
    val userBook: UserBook?
)