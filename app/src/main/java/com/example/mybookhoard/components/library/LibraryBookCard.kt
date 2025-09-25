package com.example.mybookhoard.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.components.dialogs.RemoveBookDialog
import com.example.mybookhoard.components.dialogs.WishlistSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBookCard(
    bookWithUserData: BookWithUserData,
    onReadingStatusChange: (UserBookReadingStatus) -> Unit,
    onWishlistStatusChange: (UserBookWishlistStatus) -> Unit,
    onRemoveFromCollection: () -> Unit,
    modifier: Modifier = Modifier,
    showReadingStatusButton: Boolean = true
) {
    var showActionsMenu by remember { mutableStateOf(false) }
    var showWishlistDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    val book = bookWithUserData.book
    val userBook = bookWithUserData.userBook

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Book info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!book.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Book metadata
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (book.publicationYear != null) {
                            BookMetadataChip(text = book.publicationYear.toString())
                        }
                        if (book.language.isNotBlank() && book.language != "en") {
                            BookMetadataChip(text = book.language.uppercase())
                        }
                    }
                }

                // Action button
                Box {
                    IconButton(
                        onClick = { showActionsMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions"
                        )
                    }

                    DropdownMenu(
                        expanded = showActionsMenu,
                        onDismissRequest = { showActionsMenu = false }
                    ) {
                        if (showReadingStatusButton) {
                            // Reading status options
                            DropdownMenuItem(
                                text = { Text("Mark as Reading") },
                                onClick = {
                                    onReadingStatusChange(UserBookReadingStatus.READING)
                                    showActionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Mark as Read") },
                                onClick = {
                                    onReadingStatusChange(UserBookReadingStatus.READ)
                                    showActionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Mark as Not Started") },
                                onClick = {
                                    onReadingStatusChange(UserBookReadingStatus.NOT_STARTED)
                                    showActionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )

                            HorizontalDivider()
                        }

                        // Wishlist status
                        DropdownMenuItem(
                            text = { Text("Change Wishlist Status") },
                            onClick = {
                                showWishlistDialog = true
                                showActionsMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )

                        HorizontalDivider()

                        // Remove from collection
                        DropdownMenuItem(
                            text = { Text("Remove from Collection") },
                            onClick = {
                                showRemoveDialog = true
                                showActionsMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

        }
    }

    // Dialogs
    if (showWishlistDialog) {
        WishlistSelectionDialog(
            onDismiss = { showWishlistDialog = false },
            onStatusSelected = { status ->
                onWishlistStatusChange(status)
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
private fun BookMetadataChip(
    text: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
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
private fun StatusChip(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}