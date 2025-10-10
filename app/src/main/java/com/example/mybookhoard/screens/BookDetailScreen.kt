package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.components.common.RatingSelector
import com.example.mybookhoard.components.dialogs.WishlistSelectionDialog
import com.example.mybookhoard.components.dialogs.RemoveBookDialog
import com.example.mybookhoard.data.entities.BookWithUserDataExtended
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

/**
 * Book detail screen showing title, author, and editable rating
 * Now supports both library books (with rating) and search books (with add/remove)
 * Path: app/src/main/java/com/example/mybookhoard/screens/BookDetailScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookWithUserData: BookWithUserDataExtended,
    onNavigateBack: () -> Unit,
    onRatingChange: ((userBookId: Long, Float?) -> Unit)? = null,  // ✅ Ahora opcional y con userBookId
    onAddToCollection: ((UserBookWishlistStatus) -> Unit)? = null,  // ✅ NUEVO
    onRemoveFromCollection: (() -> Unit)? = null,  // ✅ NUEVO
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val book = bookWithUserData.book
    val userBook = bookWithUserData.userBook
    val isInCollection = userBook != null  // ✅ NUEVO

    var showWishlistDialog by remember { mutableStateOf(false) }  // ✅ NUEVO
    var showRemoveDialog by remember { mutableStateOf(false) }  // ✅ NUEVO

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Book header section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Author info
                if (!bookWithUserData.authorName.isNullOrBlank()) {
                    Text(
                        text = "by ${bookWithUserData.authorName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // ✅ NUEVO: Badge si está en colección
                if (isInCollection) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "In Your Collection",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ✅ Rating selector SOLO si está en colección
            if (isInCollection && userBook != null && onRatingChange != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        RatingSelector(
                            currentRating = userBook.personalRating,
                            onRatingChange = { newRating ->
                                onRatingChange(userBook.id, newRating)
                            },
                            enabled = true
                        )
                    }
                }
            }

            // Additional book info (descripción, géneros, etc.)
            if (book.description?.isNotBlank() == true) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // ✅ NUEVO: Book metadata (genres, year, language)
            if (!book.genres.isNullOrEmpty() || book.publicationYear != null || !book.language.isNullOrBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!book.genres.isNullOrEmpty()) {
                                InfoRow(
                                    label = "Genres",
                                    value = book.genres.joinToString(", ")
                                )
                            }

                            if (book.publicationYear != null) {
                                InfoRow(
                                    label = "Published",
                                    value = book.publicationYear.toString()
                                )
                            }

                            if (!book.language.isNullOrBlank()) {
                                InfoRow(
                                    label = "Language",
                                    value = book.language.uppercase()
                                )
                            }
                        }
                    }
                }
            }

            // ✅ NUEVO: Action buttons
            if (isInCollection) {
                // Remove button
                if (onRemoveFromCollection != null) {
                    HorizontalDivider()

                    OutlinedButton(
                        onClick = { showRemoveDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove from Collection")
                    }
                }
            } else {
                // Add button
                if (onAddToCollection != null) {
                    HorizontalDivider()

                    Button(
                        onClick = { showWishlistDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Collection")
                    }
                }
            }
        }
    }

    // ✅ NUEVO: Dialogs
    if (showWishlistDialog && onAddToCollection != null) {
        WishlistSelectionDialog(
            onDismiss = { showWishlistDialog = false },
            onStatusSelected = { status ->
                onAddToCollection(status)
                showWishlistDialog = false
            }
        )
    }

    if (showRemoveDialog && onRemoveFromCollection != null) {
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

// ✅ NUEVO: Helper composable
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
}