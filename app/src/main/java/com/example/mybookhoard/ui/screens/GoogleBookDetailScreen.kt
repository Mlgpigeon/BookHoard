// File: app/src/main/java/com/example/mybookhoard/ui/screens/GoogleBookDetailScreen.kt
// NEW FILE: Screen to show Google Books details before adding to library

package com.example.mybookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.api.SearchResult
import com.example.mybookhoard.data.WishlistStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleBookDetailScreen(
    googleBook: SearchResult,
    vm: BooksVm,
    onNavigateBack: () -> Unit,
    onBookAdded: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add to Library") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Book Header Section
            GoogleBookHeaderSection(googleBook = googleBook)

            // Source Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Source: ${googleBook.sourceLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "This book is not yet in your library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Description Section
            if (!googleBook.description.isNullOrBlank()) {
                GoogleBookDescriptionSection(description = googleBook.description)
            }

            // Additional Info Section
            GoogleBookInfoSection(googleBook = googleBook)

            // Add some space at the bottom for the FAB
            Spacer(Modifier.height(80.dp))
        }
    }

    // Add Book Dialog
    if (showAddDialog) {
        AddGoogleBookDialog(
            book = googleBook.toApiBook().toLocalBook(),
            onDismiss = { showAddDialog = false },
            onConfirm = { wishlistStatus ->
                vm.addGoogleBook(
                    title = googleBook.title,
                    author = googleBook.author,
                    saga = googleBook.saga,
                    description = googleBook.description,
                    wishlistStatus = wishlistStatus
                )
                showAddDialog = false
                onBookAdded()
            }
        )
    }
}

@Composable
private fun GoogleBookHeaderSection(googleBook: SearchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Title
            Text(
                text = googleBook.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Author
            googleBook.author?.let { author ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "by $author",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Saga
            googleBook.saga?.let { saga ->
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = saga,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleBookDescriptionSection(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoogleBookInfoSection(googleBook: SearchResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Book Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            // Source
            InfoRow(
                label = "Source",
                value = googleBook.sourceLabel,
                icon = Icons.Default.Source
            )

            // Google Books ID (if available)
            googleBook.googleBooksId?.let { id ->
                InfoRow(
                    label = "Google Books ID",
                    value = id,
                    icon = Icons.Default.Tag
                )
            }

            // Status in library
            InfoRow(
                label = "Library Status",
                value = "Not in your library",
                icon = Icons.Default.LibraryBooks
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddGoogleBookDialog(
    book: com.example.mybookhoard.data.Book,
    onDismiss: () -> Unit,
    onConfirm: (WishlistStatus) -> Unit
) {
    var selectedWishlistStatus by remember { mutableStateOf(WishlistStatus.WISH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Library") },
        text = {
            Column {
                Text(
                    text = "Add \"${book.title}\" to your library?",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Wishlist Status:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                WishlistStatus.entries.forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedWishlistStatus == status,
                            onClick = { selectedWishlistStatus = status }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (status) {
                                WishlistStatus.WISH -> "Wishlist"
                                WishlistStatus.ON_THE_WAY -> "On the way"
                                WishlistStatus.OBTAINED -> "Obtained"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedWishlistStatus) }) {
                Text("Add Book")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}