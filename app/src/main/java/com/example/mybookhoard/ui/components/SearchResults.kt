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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus

@Composable
fun SearchResults(
    vm: BooksVm,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by vm.searchQuery.collectAsState()
    val filteredBooks by vm.filteredBooks.collectAsState(initial = emptyList())

    // Separate books by those matching author vs title/other criteria
    val booksByAuthor = remember(filteredBooks, searchQuery) {
        filteredBooks.filter { book ->
            book.author?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val booksByOther = remember(filteredBooks, searchQuery) {
        filteredBooks.filter { book ->
            book.author?.contains(searchQuery, ignoreCase = true) != true
        }
    }

    Column(modifier = modifier) {
        // Search summary header
        SearchResultsHeader(
            searchQuery = searchQuery,
            totalResults = filteredBooks.size
        )

        if (filteredBooks.isEmpty()) {
            NoResultsFound(searchQuery = searchQuery)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Books by matching authors
                if (booksByAuthor.isNotEmpty()) {
                    item {
                        CategoryHeader(
                            title = "By Author",
                            subtitle = "${booksByAuthor.size} book${if (booksByAuthor.size != 1) "s" else ""}",
                            icon = Icons.Default.Person
                        )
                    }
                    items(booksByAuthor) { book ->
                        SearchResultBookCard(
                            book = book,
                            searchQuery = searchQuery,
                            onClick = { onBookClick(book) },
                            vm = vm
                        )
                    }

                    if (booksByOther.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                // Books by title/saga/description
                if (booksByOther.isNotEmpty()) {
                    item {
                        CategoryHeader(
                            title = "By Title & Content",
                            subtitle = "${booksByOther.size} book${if (booksByOther.size != 1) "s" else ""}",
                            icon = Icons.Default.MenuBook
                        )
                    }
                    items(booksByOther) { book ->
                        SearchResultBookCard(
                            book = book,
                            searchQuery = searchQuery,
                            onClick = { onBookClick(book) },
                            vm = vm
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(
    searchQuery: String,
    totalResults: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "$totalResults result${if (totalResults != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        Divider(
            modifier = Modifier.width(60.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultBookCard(
    book: Book,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!book.author.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "by ${book.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!book.saga.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "📚 ${book.saga}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Status indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reading status
                    StatusChip(
                        text = when (book.status) {
                            ReadingStatus.NOT_STARTED -> "Unread"
                            ReadingStatus.READING -> "Reading"
                            ReadingStatus.READ -> "Read"
                        },
                        color = when (book.status) {
                            ReadingStatus.NOT_STARTED -> MaterialTheme.colorScheme.outline
                            ReadingStatus.READING -> MaterialTheme.colorScheme.secondary
                            ReadingStatus.READ -> MaterialTheme.colorScheme.primary
                        }
                    )

                    // Wishlist status
                    if (book.wishlist != null) {
                        StatusChip(
                            text = when (book.wishlist) {
                                WishlistStatus.WISH -> "⭐ Wish"
                                WishlistStatus.ON_THE_WAY -> "📦 On the Way"
                                WishlistStatus.OBTAINED -> "📚 Obtained"
                            },
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Action menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Details") },
                        onClick = {
                            onClick()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                        }
                    )

                    Divider()

                    // Quick status actions
                    DropdownMenuItem(
                        text = { Text("Mark as Reading") },
                        onClick = {
                            vm.updateStatus(book, ReadingStatus.READING)
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AutoStories, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Mark as Read") },
                        onClick = {
                            vm.updateStatus(book, ReadingStatus.READ)
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

@Composable
private fun StatusChip(
    text: String,
    color: androidx.compose.ui.graphics.Color
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