package com.example.mybookhoard.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybookhoard.components.common.LoadingIndicator
import com.example.mybookhoard.components.library.LibraryStatsCard
import com.example.mybookhoard.components.library.ExpandableBookSection
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.viewmodels.LibraryViewModel

@Composable
fun MyLibraryTab(
    libraryViewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    val libraryStats by libraryViewModel.libraryStats.collectAsState()
    val readBooks by libraryViewModel.readBooks.collectAsState()
    val readingBooks by libraryViewModel.readingBooks.collectAsState()
    val notStartedBooks by libraryViewModel.notStartedBooks.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()

    if (isLoading) {
        LoadingIndicator(
            modifier = modifier.fillMaxSize(),
            message = "Loading your library..."
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Library stats banner
            item {
                LibraryStatsCard(stats = libraryStats)
            }

            // Reading section
            item {
                ExpandableBookSection(
                    title = "Currently Reading",
                    books = readingBooks,
                    onReadingStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateReadingStatus(bookId, newStatus)
                    },
                    onWishlistStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateWishlistStatus(bookId, newStatus)
                    },
                    onRemoveFromCollection = { bookId ->
                        libraryViewModel.removeBookFromCollection(bookId)
                    }
                )
            }

            // Not started section
            item {
                ExpandableBookSection(
                    title = "Not Started",
                    books = notStartedBooks,
                    onReadingStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateReadingStatus(bookId, newStatus)
                    },
                    onWishlistStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateWishlistStatus(bookId, newStatus)
                    },
                    onRemoveFromCollection = { bookId ->
                        libraryViewModel.removeBookFromCollection(bookId)
                    }
                )
            }

            // Read section
            item {
                ExpandableBookSection(
                    title = "Finished Reading",
                    books = readBooks,
                    onReadingStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateReadingStatus(bookId, newStatus)
                    },
                    onWishlistStatusChange = { bookId, newStatus ->
                        libraryViewModel.updateWishlistStatus(bookId, newStatus)
                    },
                    onRemoveFromCollection = { bookId ->
                        libraryViewModel.removeBookFromCollection(bookId)
                    }
                )
            }
        }
    }
}