package com.example.mybookhoard.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.components.common.LoadingIndicator
import com.example.mybookhoard.components.library.ExpandableBookSection
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.viewmodels.LibraryViewModel

@Composable
fun MyWishlistTab(
    libraryViewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    val wishlistBooks by libraryViewModel.wishlistBooks.collectAsState()
    val onTheWayBooks by libraryViewModel.onTheWayBooks.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()
    val wishlistSearchQuery by libraryViewModel.wishlistSearchQuery.collectAsState()

    if (isLoading) {
        LoadingIndicator(
            modifier = modifier.fillMaxSize(),
            message = "Loading your wishlist..."
        )
    } else {
        val totalWishlistBooks = wishlistBooks.size + onTheWayBooks.size

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar
            item {
                WishlistSearchBar(
                    query = wishlistSearchQuery,
                    onQueryChange = libraryViewModel::updateWishlistSearchQuery,
                    onClearSearch = libraryViewModel::clearWishlistSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Wishlist summary
            item {
                WishlistSummaryCard(
                    totalBooks = totalWishlistBooks,
                    searchQuery = wishlistSearchQuery
                )
            }

            // On The Way section
            item {
                ExpandableBookSection(
                    title = "On The Way",
                    books = onTheWayBooks,
                    onReadingStatusChange = { _, _ ->
                        // Not applicable for wishlist items
                    },
                    onWishlistStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateWishlistStatus(bookId, newStatus)
                    },
                    onRemoveFromCollection = { bookId ->
                        libraryViewModel.removeBookFromCollection(bookId)
                    },
                    showReadingStatusButton = false
                )
            }

            // Wishlist section
            item {
                ExpandableBookSection(
                    title = "Wishlist",
                    books = wishlistBooks,
                    onReadingStatusChange = { _, _ ->
                        // Not applicable for wishlist items
                    },
                    onWishlistStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateWishlistStatus(bookId, newStatus)
                    },
                    onRemoveFromCollection = { bookId ->
                        libraryViewModel.removeBookFromCollection(bookId)
                    },
                    showReadingStatusButton = false
                )
            }

            // Empty state
            if (totalWishlistBooks == 0) {
                item {
                    if (wishlistSearchQuery.isBlank()) {
                        EmptyWishlistState()
                    } else {
                        EmptySearchState(onClearSearch = libraryViewModel::clearWishlistSearch)
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                "Search your wishlist...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty())
                    MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun WishlistSummaryCard(
    totalBooks: Int,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (searchQuery.isBlank()) {
            CardDefaults.elevatedCardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (searchQuery.isBlank()) {
                    androidx.compose.material.icons.Icons.Default.Star
                } else {
                    Icons.Default.Search
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (searchQuery.isBlank()) "My Wishlist" else "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (searchQuery.isBlank()) {
                        "$totalBooks book${if (totalBooks != 1) "s" else ""} on your wishlist"
                    } else {
                        "$totalBooks result${if (totalBooks != 1) "s" else ""} for \"$searchQuery\""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyWishlistState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Wishlist is Empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Search for books and add them to your wishlist to keep track of books you want to read",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Results Found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Try adjusting your search terms",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = onClearSearch) {
                Text("Clear Search")
            }
        }
    }
}