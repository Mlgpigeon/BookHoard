package com.example.mybookhoard.ui.components.form

import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus

data class BookFormState(
    val title: String = "",
    val author: String = "",
    val saga: String = "",
    val description: String = "",
    val readingStatus: ReadingStatus = ReadingStatus.NOT_STARTED,
    val wishlistStatus: WishlistStatus? = null
)

fun BookFormState.isValid(): Boolean = title.isNotBlank()

fun BookFormState.hasChanges(original: BookFormState): Boolean {
    return title != original.title ||
            author != original.author ||
            saga != original.saga ||
            description != original.description ||
            readingStatus != original.readingStatus ||
            wishlistStatus != original.wishlistStatus
}