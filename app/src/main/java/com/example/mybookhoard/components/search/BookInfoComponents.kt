package com.example.mybookhoard.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

/**
 * Book card UI components for search functionality
 * Path: app/src/main/java/com/example/mybookhoard/components/search/BookInfoComponents.kt
 */

@Composable
fun BookTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun BookAuthor(
    authorName: String?,
    modifier: Modifier = Modifier
) {
    if (!authorName.isNullOrBlank()) {
        Text(
            text = "by $authorName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

@Composable
fun BookDescription(
    description: String?,
    maxLines: Int = 3,
    modifier: Modifier = Modifier
) {
    if (!description.isNullOrBlank()) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

@Composable
fun BookInfoChip(
    text: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { /* Future: Filter by this criteria */ },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier
    )
}

@Composable
fun BookMetadataRow(
    book: Book,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (book.publicationYear != null) {
            BookInfoChip(text = book.publicationYear.toString())
        }
        if (book.language.isNotBlank() && book.language != "en") {
            BookInfoChip(text = book.language.uppercase())
        }
        if (!book.genres.isNullOrEmpty()) {
            BookInfoChip(text = "${book.genres.size} genre${if (book.genres.size != 1) "s" else ""}")
        }
        // Show source for non-user-defined books
        if (book.source.name != "USER_DEFINED") {
            BookInfoChip(
                text = when (book.source.name) {
                    "GOOGLE_BOOKS_API" -> "Google Books"
                    "OPENLIBRARY_API" -> "Open Library"
                    else -> book.source.name
                }
            )
        }
    }
}

@Composable
fun CollectionButton(
    isInCollection: Boolean,
    wishlistStatus: UserBookWishlistStatus?,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isInCollection) {
        FilledTonalButton(
            onClick = onRemoveClick,
            modifier = modifier,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (wishlistStatus) {
                    UserBookWishlistStatus.WISH -> "Wished"
                    UserBookWishlistStatus.ON_THE_WAY -> "On The Way"
                    UserBookWishlistStatus.OBTAINED -> "Obtained"
                    null -> "Added"
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onAddClick,
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}