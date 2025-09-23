// Replace app/src/main/java/com/example/mybookhoard/ui/components/SearchResults.kt

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

        // Error message if any
        searchError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (combinedResults == null && searchQuery.isBlank()) {
            EmptySearchState()
        } else if (combinedResults?.totalResults == 0) {
            NoResultsFound(searchQuery = searchQuery)
        } else {
            combinedResults?.let { results ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Local results section
                    if (results.localResults.isNotEmpty()) {
                        item {
                            CategoryHeader(
                                title = "Your Library",
                                subtitle = "${results.localResults.size} book${if (results.localResults.size != 1) "s" else ""}",
                                icon = Icons.Default.MenuBook
                            )
                        }
                        items(results.localResults) { searchResult ->
                            SearchResultCard(
                                searchResult = searchResult,
                                searchQuery = searchQuery,
                                onClick = {
                                    // Convert SearchResult back to Book for onClick
                                    val book = searchResult.toApiBook().toLocalBook()
                                    onBookClick(book)
                                },
                                vm = vm
                            )
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
                                onAddToLibrary = { book ->
                                    // TODO: Implement add to library functionality
                                }
                            )
                        }
                    }
                }
            }
        }
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
            Text(
                text = "for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (localCount > 0 || googleCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row {
                    if (localCount > 0) {
                        SourceChip(
                            text = "$localCount Your Library",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (googleCount > 0) {
                        if (localCount > 0) Spacer(Modifier.width(8.dp))
                        SourceChip(
                            text = "$googleCount Google Books",
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
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.width(60.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultCard(
    searchResult: SearchResult,
    searchQuery: String,
    onClick: () -> Unit,
    vm: BooksVm
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
                        color = MaterialTheme.colorScheme.primary
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

                // Status chip for local books
                if (searchResult.source == "local" && searchResult.status != null) {
                    val statusText = when (searchResult.status) {
                        "NOT_STARTED" -> "Unread"
                        "READING" -> "Reading"
                        "READ" -> "Read"
                        else -> searchResult.status
                    }
                    val statusColor = when (searchResult.status) {
                        "NOT_STARTED" -> MaterialTheme.colorScheme.outline
                        "READING" -> MaterialTheme.colorScheme.primary
                        "READ" -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.outline
                    }

                    StatusChip(
                        text = statusText,
                        color = statusColor
                    )
                }
            }

            // Menu for local books only
            if (searchResult.source == "local") {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as Reading") },
                            onClick = {
                                // TODO: Update status
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark as Read") },
                            onClick = {
                                // TODO: Update status
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoResultsFound(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No results found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Try adjusting your search:\n• Check spelling\n• Use different keywords\n• Search by author or saga",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Searched for: \"$searchQuery\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}