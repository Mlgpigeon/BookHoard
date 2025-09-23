// Replace the entire app/src/main/java/com/example/mybookhoard/ui/components/SearchResults.kt

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
                                    val book = searchResult.toApiBook().toLocalBook()
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
                            icon = Icons.Default.CloudSync
                        )
                    }
                    items(results.googleResults) { searchResult ->
                        GoogleBookCard(
                            searchResult = searchResult,
                            searchQuery = searchQuery,
                            onAddToLibrary = { googleBook ->
                                selectedGoogleBook = googleBook
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        } else if (searchQuery.isNotBlank() && !isSearchingGoogle) {
            EmptySearchState()
        }
    }

    // Google Book Add Dialog
    if (showAddDialog && selectedGoogleBook != null) {
        GoogleBookAddDialog(
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
                Text(
                    text = "for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (localCount > 0 || googleCount > 0) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (localCount > 0) {
                        StatusChip(
                            text = "$localCount in your library",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (googleCount > 0) {
                        StatusChip(
                            text = "$googleCount from Google Books",
                            color = MaterialTheme.colorScheme.secondary
                        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        val statusColor = when (ReadingStatus.valueOf(status)) {
                            ReadingStatus.READ -> MaterialTheme.colorScheme.primary
                            ReadingStatus.READING -> MaterialTheme.colorScheme.secondary
                            ReadingStatus.NOT_STARTED -> MaterialTheme.colorScheme.tertiary

                        }
                        StatusChip(text = status.replace("_", " "), color = statusColor)
                    }

                    searchResult.wishlist?.let { wishlist ->
                        val wishlistColor = when (WishlistStatus.valueOf(wishlist)) {
                            WishlistStatus.WISH -> MaterialTheme.colorScheme.secondary
                            WishlistStatus.ON_THE_WAY -> MaterialTheme.colorScheme.tertiary
                            WishlistStatus.OBTAINED -> MaterialTheme.colorScheme.primary
                        }
                        StatusChip(text = wishlist.replace("_", " "), color = wishlistColor)
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
    onAddToLibrary: (SearchResult) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title and source
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
                Text("Add", style = MaterialTheme.typography.labelSmall)
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
        color = color.copy(alpha = 0.1f),
        modifier = Modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Search your library and Google Books",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Find books by title, author, or saga.\nResults include your library and Google Books!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}