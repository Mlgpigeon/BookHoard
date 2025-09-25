// File: app/src/main/java/com/example/mybookhoard/ui/components/search/SearchResults.kt
// Modularización basada ÚNICAMENTE en el código existente

package com.example.mybookhoard.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    onGoogleBookClick: (SearchResult) -> Unit = {},
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
            isSearchingGoogle = isSearchingGoogle
        )

        // Error handling
        searchError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Search error: $error",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Results list
        when {
            combinedResults == null && !isSearchingGoogle -> {
                // No results state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Enter a search term to find books"
                        } else {
                            "No books found for \"$searchQuery\""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Local books from combinedResults.localResults
                    combinedResults?.localResults?.let { localResults ->
                        items(localResults, key = { it.id ?: 0L }) { searchResult ->
                            LocalBookItem(
                                searchResult = searchResult,
                                onClick = {
                                    // Convert SearchResult back to Book for navigation
                                    searchResult.id?.let { id ->
                                        val book = Book(
                                            id = id,
                                            title = searchResult.title,
                                            author = searchResult.author,
                                            saga = searchResult.saga,
                                            description = searchResult.description,
                                            status = searchResult.status?.let {
                                                try { ReadingStatus.valueOf(it) }
                                                catch(e: Exception) { ReadingStatus.NOT_STARTED }
                                            } ?: ReadingStatus.NOT_STARTED,
                                            wishlist = searchResult.wishlist?.let {
                                                try { WishlistStatus.valueOf(it) }
                                                catch(e: Exception) { null }
                                            }
                                        )
                                        onBookClick(book)
                                    }
                                }
                            )
                        }
                    }

                    // Google Books results from combinedResults.googleResults
                    combinedResults?.googleResults?.let { googleResults ->
                        items(googleResults, key = { it.googleBooksId ?: it.title }) { searchResult ->
                            GoogleBookItem(
                                searchResult = searchResult,
                                onClick = { onGoogleBookClick(searchResult) },
                                onAddToLibrary = { result ->
                                    selectedGoogleBook = result
                                    showAddDialog = true
                                }
                            )
                        }
                    }

                    // Loading indicator for Google search
                    if (isSearchingGoogle) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Searching online...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Google Book Dialog
    selectedGoogleBook?.let { googleBook ->
        if (showAddDialog) {
            AddGoogleBookDialog(
                searchResult = googleBook,
                onDismiss = {
                    showAddDialog = false
                    selectedGoogleBook = null
                },
                onConfirm = { wishlistStatus ->
                    vm.addGoogleBook(
                        title = googleBook.title,
                        author = googleBook.author,
                        saga = googleBook.saga,
                        description = googleBook.description,
                        wishlistStatus = wishlistStatus
                    )
                    showAddDialog = false
                    selectedGoogleBook = null
                }
            )
        }
    }
}