package com.example.mybookhoard.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.components.common.LoadingIndicator
import com.example.mybookhoard.components.library.LibraryStatsCard
import com.example.mybookhoard.components.library.ExpandableBookSection
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.viewmodels.LibraryViewModel
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLibraryTab(
    libraryViewModel: LibraryViewModel,
    onNavigateToBookDetail: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryStats by libraryViewModel.libraryStats.collectAsState()
    val readBooks by libraryViewModel.readBooks.collectAsState()
    val readingBooks by libraryViewModel.readingBooks.collectAsState()
    val notStartedBooks by libraryViewModel.notStartedBooks.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()
    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()

    if (isLoading) {
        LoadingIndicator(
            modifier = modifier.fillMaxSize(),
            message = "Loading your library..."
        )
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { libraryViewModel.refresh() },
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search bar
                item {
                    LibrarySearchBar(
                        query = searchQuery,
                        onQueryChange = libraryViewModel::updateSearchQuery,
                        onClearSearch = libraryViewModel::clearSearch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Library stats banner
                item {
                    LibraryStatsCard(stats = libraryStats)
                }
                if (readingBooks.isNotEmpty()) {
                    item {
                        ExpandableBookSection(
                            title = "Reading",
                            books = readingBooks,
                            onReadingStatusChange = libraryViewModel::updateReadingStatus,
                            onWishlistStatusChange = libraryViewModel::updateWishlistStatus,
                            onRemoveFromCollection = libraryViewModel::removeBookFromCollection,
                            onBookClick = { bookWithUserData ->
                                bookWithUserData.userBook?.id?.let { userBookId ->
                                    onNavigateToBookDetail(userBookId, bookWithUserData.book.id)
                                }
                            },
                            showReadingStatusButton = true,
                            backgroundColor = Color(0xFFB3D9FF)
                        )
                    }
                }

                // Not Started section - only show if not empty
                if (notStartedBooks.isNotEmpty()) {
                    item {
                        ExpandableBookSection(
                            title = "Not Started",
                            books = notStartedBooks,
                            onReadingStatusChange = libraryViewModel::updateReadingStatus,
                            onWishlistStatusChange = libraryViewModel::updateWishlistStatus,
                            onRemoveFromCollection = libraryViewModel::removeBookFromCollection,
                            onBookClick = { book ->  // AÑADIR ESTE PARÁMETRO
                                val userBookId = book.userBook?.id ?: return@ExpandableBookSection
                                onNavigateToBookDetail(userBookId, book.book.id)
                            },
                            showReadingStatusButton = true,
                            backgroundColor = Color(0xFFFFD4B3) // Soft pastel peach
                        )
                    }
                }

                // Read section - only show if not empty
                if (readBooks.isNotEmpty()) {
                    item {
                        ExpandableBookSection(
                            title = "Read",
                            books = readBooks,
                            onReadingStatusChange = libraryViewModel::updateReadingStatus,
                            onWishlistStatusChange = libraryViewModel::updateWishlistStatus,
                            onRemoveFromCollection = libraryViewModel::removeBookFromCollection,
                            onBookClick = { book ->  // AÑADIR ESTE PARÁMETRO
                                val userBookId = book.userBook?.id ?: return@ExpandableBookSection
                                onNavigateToBookDetail(userBookId, book.book.id)
                            },
                            showReadingStatusButton = false,
                            backgroundColor = Color(0xFFB8E6B8) // Soft pastel green
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(
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
                "Search your library...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty())
                    MaterialTheme.colorScheme.primary
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
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)
    )
}