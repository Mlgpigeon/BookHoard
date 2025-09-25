package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybookhoard.components.common.EmptySearchState
import com.example.mybookhoard.components.search.SearchBar
import com.example.mybookhoard.components.search.BookSearchCard
import com.example.mybookhoard.components.common.LoadingIndicator
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.example.mybookhoard.viewmodels.SearchViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel()
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val suggestions by searchViewModel.suggestions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchViewModel.updateSearchQuery(it) },
            onSearch = { searchViewModel.performSearch() },
            suggestions = suggestions,
            onSuggestionSelected = { suggestion ->
                searchViewModel.selectSuggestion(suggestion)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is SearchViewModel.SearchUiState.Initial -> {
                EmptySearchState(
                    title = "Search Your Library",
                    subtitle = "Find books in your collection or discover new ones",
                    modifier = Modifier.fillMaxSize()
                )
            }

            is SearchViewModel.SearchUiState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize()
                )
            }

            is SearchViewModel.SearchUiState.Success -> {
                if (state.books.isEmpty()) {
                    EmptySearchState(
                        title = "No Results Found",
                        subtitle = "Try adjusting your search terms",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.books) { bookWithUserData ->
                            BookSearchCard(
                                book = bookWithUserData.book,
                                userBook = bookWithUserData.userBook,
                                onAddToCollection = { wishlistStatus ->
                                    searchViewModel.addBookToCollection(
                                        bookWithUserData.book,
                                        wishlistStatus
                                    )
                                },
                                onRemoveFromCollection = {
                                    searchViewModel.removeBookFromCollection(bookWithUserData.book.id)
                                }
                            )
                        }
                    }
                }
            }

            is SearchViewModel.SearchUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Search Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { searchViewModel.retrySearch() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}