package com.example.mybookhoard.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
            // Wishlist summary
            item {
                WishlistSummaryCard(totalBooks = totalWishlistBooks)
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

            // On The Way section
            item {
                ExpandableBookSection(
                    title = "On The Way",
                    books = onTheWayBooks,
                    onReadingStatusChange = { _, _ ->
                        // Not applicable for on the way items
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

            // Empty state when no wishlist items
            if (totalWishlistBooks == 0) {
                item {
                    EmptyWishlistState()
                }
            }
        }
    }
}

@Composable
private fun WishlistSummaryCard(
    totalBooks: Int,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My Wishlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$totalBooks book${if (totalBooks != 1) "s" else ""} on your wishlist",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
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