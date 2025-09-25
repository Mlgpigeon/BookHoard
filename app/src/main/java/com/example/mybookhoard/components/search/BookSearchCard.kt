package com.example.mybookhoard.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.components.dialogs.RemoveBookDialog
import com.example.mybookhoard.components.dialogs.WishlistSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchCard(
    book: Book,
    userBook: UserBook?,
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
            // Book title and author
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (book.primaryAuthorId != null) {
                // TODO: In future, resolve author name from ID
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Author ID: ${book.primaryAuthorId}", // Placeholder
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!book.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Book metadata
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Book info chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (book.publicationYear != null) {
                        BookInfoChip(text = book.publicationYear.toString())
                    }
                    if (!book.language.isBlank() && book.language != "en") {
                        BookInfoChip(text = book.language.uppercase())
                    }
                    if (!book.genres.isNullOrEmpty()) {
                        BookInfoChip(text = "${book.genres.size} genres")
                    }
                }

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

@Composable
private fun BookInfoChip(
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
private fun CollectionButton(
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