package com.example.mybookhoard.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.components.dialogs.RemoveBookDialog
import com.example.mybookhoard.components.dialogs.WishlistSelectionDialog

/**
 * Modularized Book Search Card Component
 * Path: app/src/main/java/com/example/mybookhoard/components/search/BookSearchCard.kt
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchCard(
    book: Book,
    userBook: UserBook?,
    authorName: String? = null, // For API books that come with author name
    onAddToCollection: (UserBookWishlistStatus) -> Unit,
    onRemoveFromCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWishlistDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    val isInCollection = userBook != null

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            // Future: Navigate to book details
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Book title
            BookTitle(
                title = book.title,
                modifier = Modifier.fillMaxWidth()
            )

            // Author - could be from API (authorName) or original title field
            val displayAuthor = authorName ?: book.originalTitle // API books may use originalTitle for author
            if (!displayAuthor.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                BookAuthor(
                    authorName = displayAuthor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Description
            if (!book.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                BookDescription(
                    description = book.description,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Book metadata and action button
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Book info chips
                BookMetadataRow(
                    book = book,
                    modifier = Modifier.weight(1f)
                )

                // Add/Remove button
                CollectionButton(
                    isInCollection = isInCollection,
                    wishlistStatus = userBook?.wishlistStatus,
                    onAddClick = { showWishlistDialog = true },
                    onRemoveClick = { showRemoveDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showWishlistDialog) {
        WishlistSelectionDialog(
            onDismiss = { showWishlistDialog = false },
            onStatusSelected = { status ->
                onAddToCollection(status)
                showWishlistDialog = false
            }
        )
    }

    if (showRemoveDialog) {
        RemoveBookDialog(
            bookTitle = book.title,
            onConfirm = {
                onRemoveFromCollection()
                showRemoveDialog = false
            },
            onDismiss = { showRemoveDialog = false }
        )
    }
}