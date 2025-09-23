// File: app/src/main/java/com/example/mybookhoard/ui/components/SearchResults.kt
// Changes: Only modify the onBookClick callback for local books to handle navigation properly

package com.example.mybookhoard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus
import com.example.mybookhoard.api.SearchResult

@Composable
fun SearchResults(
    vm: BooksVm,
    onBookClick: (Book) -> Unit,
    onGoogleBookClick: (SearchResult) -> Unit = {}, // NEW: For Google Book details
    modifier: Modifier = Modifier
) {
    val searchQuery by vm.searchQuery.collectAsState()
    val combinedResults by vm.searchVm.combinedSearchResults.collectAsState()
    val isSearchingGoogle by vm.searchVm.isSearchingGoogle.collectAsState()
    val searchError by vm.searchVm.searchError.collectAsState()

    // State for Google Book Add Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoogleBook by remember { mutableStateOf<SearchResult?>(null) }

    // Trigger Google Books search when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            vm.searchVm.searchWithGoogleBooks(searchQuery)
        } else {
            vm.searchVm.clearGoogleSearch()
        }
    }

    Column(modifier = modifier) {
        // Search summary header
        SearchResultsHeader(
            searchQuery = searchQuery,
            totalResults = combinedResults?.totalResults ?: 0,
            localCount = combinedResults?.totalLocal ?: 0,
            googleCount = combinedResults?.totalGoogle ?: 0,
            isLoading = isSearchingGoogle
        )

        // Error display
        searchError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Results content
        val results = combinedResults
        if (results != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Local results section
                if (results.localResults.isNotEmpty()) {
                    item {
                        CategoryHeader(
                            title = "Your Library",
                            subtitle = "${results.localResults.size} result${if (results.localResults.size != 1) "s" else ""}",
                            icon = Icons.Default.BookmarkBorder
                        )
                    }
                    items(results.localResults) { searchResult ->
                        if (searchResult.id != null) {
                            LocalBookCard(
                                searchResult = searchResult,
                                searchQuery = searchQuery,
                                onBookClick = {
                                    // FIXED: Use the existing ID to navigate directly to BookDetail
                                    // Create a minimal Book object just for navigation
                                    val book = Book(
                                        id = searchResult.id,
                                        title = searchResult.title,
                                        author = searchResult.author ?: "",
                                        saga = searchResult.saga,
                                        description = searchResult.description,
                                        status = ReadingStatus.valueOf(searchResult.status ?: "NOT_STARTED"),
                                        wishlist = searchResult.wishlist?.let { WishlistStatus.valueOf(it) }
                                    )
                                    onBookClick(book)
                                },
                                vm = vm
                            )
                        }
                    }
                }

                // Google Books results section
                if (results.googleResults.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        CategoryHeader(
                            title = "Google Books",
                            subtitle = "${results.googleResults.size} result${if (results.googleResults.size != 1) "s" else ""}",
                            icon = Icons.Default.CloudCircle
                        )
                    }
                    items(results.googleResults) { searchResult ->
                        GoogleBookCard(
                            searchResult = searchResult,
                            searchQuery = searchQuery,
                            onBookClick = { onGoogleBookClick(searchResult) }, // NEW: Make whole card clickable
                            onAddToLibrary = { googleBook ->
                                selectedGoogleBook = googleBook
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        } else if (searchQuery.isNotBlank()) {
            // Show loading or empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchingGoogle) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Searching...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Google Book Add Dialog
    if (showAddDialog && selectedGoogleBook != null) {
        AddGoogleBookDialog(
            book = selectedGoogleBook!!.toApiBook().toLocalBook(),
            onDismiss = {
                showAddDialog = false
                selectedGoogleBook = null
            },
            onConfirm = { wishlistStatus ->
                selectedGoogleBook?.let { googleBook ->
                    vm.addGoogleBook(
                        title = googleBook.title,
                        author = googleBook.author,
                        saga = googleBook.saga,
                        description = googleBook.description,
                        wishlistStatus = wishlistStatus
                    )
                }
                showAddDialog = false
                selectedGoogleBook = null
            }
        )
    }
}

@Composable
private fun SearchResultsHeader(
    searchQuery: String,
    totalResults: Int,
    localCount: Int,
    googleCount: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$totalResults result${if (totalResults != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLoading) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Searching for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (totalResults > 0) {
                Spacer(Modifier.height(8.dp))
                Row {
                    if (localCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("$localCount in library")
                        }
                    }
                    if (googleCount > 0) {
                        if (localCount > 0) Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text("$googleCount on Google")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocalBookCard(
    searchResult: SearchResult,
    searchQuery: String,
    onBookClick: () -> Unit,
    vm: BooksVm
) {
    ElevatedCard(
        onClick = onBookClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = searchResult.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SourceChip(
                        text = searchResult.sourceLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                searchResult.author?.let { author ->
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                searchResult.saga?.let { saga ->
                    Text(
                        text = "📚 $saga",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Status indicators
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searchResult.status?.let { status ->
                        StatusChip(
                            text = when (status) {
                                "NOT_STARTED" -> "Not Started"
                                "READING" -> "Reading"
                                "READ" -> "Read"
                                else -> status
                            },
                            color = when (status) {
                                "READ" -> MaterialTheme.colorScheme.primary
                                "READING" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }

                    searchResult.wishlist?.let { wishlist ->
                        StatusChip(
                            text = when (wishlist) {
                                "WISH" -> "Wishlist"
                                "ON_THE_WAY" -> "On the way"
                                "OBTAINED" -> "Obtained"
                                else -> wishlist
                            },
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleBookCard(
    searchResult: SearchResult,
    searchQuery: String,
    onBookClick: () -> Unit, // NEW: Make whole card clickable
    onAddToLibrary: (SearchResult) -> Unit
) {
    ElevatedCard(
        onClick = onBookClick, // NEW: Make whole card clickable
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = searchResult.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SourceChip(
                        text = searchResult.sourceLabel,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Author
                searchResult.author?.let { author ->
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Saga
                searchResult.saga?.let { saga ->
                    Text(
                        text = "📚 $saga",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Description
                searchResult.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Add to library button
            OutlinedButton(
                onClick = { onAddToLibrary(searchResult) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

@Composable
private fun SourceChip(
    text: String,
    color: Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AddGoogleBookDialog(
    book: Book,
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