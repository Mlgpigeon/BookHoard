package com.example.mybookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.api.SearchResult
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.ui.components.SearchBar
import com.example.mybookhoard.ui.components.search.SearchResults

@Composable
fun SearchScreen(
    vm: BooksVm,
    onNavigateBack: () -> Unit,
    onBookClick: (Book) -> Unit,
    onGoogleBookClick: (SearchResult) -> Unit = {} // NEW: For Google Book navigation
) {
    var showFullResults by remember { mutableStateOf(false) }
    var searchExecuted by remember { mutableStateOf(false) }

    val searchQuery by vm.searchQuery.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search bar with live suggestions
        SearchBar(
            vm = vm,
            onSearchExecuted = {
                showFullResults = true
                searchExecuted = true
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Show appropriate content based on state
        when {
            showFullResults && searchExecuted -> {
                // Full search results page
                SearchResults(
                    vm = vm,
                    onBookClick = onBookClick,
                    onGoogleBookClick = onGoogleBookClick, // NEW: Pass the callback
                    modifier = Modifier.fillMaxSize()
                )
            }
            searchQuery.isBlank() -> {
                // Empty state with helpful message
                EmptySearchState()
            }
            else -> {
                // Live suggestions are shown in SearchBar component
                Spacer(Modifier.height(16.dp))
            }
        }
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
            text = "Search your library",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Find books by title, author, or saga.\nSearch is smart and flexible!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}