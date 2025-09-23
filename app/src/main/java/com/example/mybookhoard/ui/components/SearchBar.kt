package com.example.mybookhoard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.utils.FuzzySearchUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    vm: BooksVm,
    onSearchExecuted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by vm.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current

    var showSuggestions by remember { mutableStateOf(false) }
    var authorSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var bookSuggestions by remember { mutableStateOf<List<Book>>(emptyList()) }

    // Get all books for suggestions
    val allBooks by vm.items.collectAsState(initial = emptyList())

    // Update suggestions when search query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            // Get author suggestions
            vm.searchAuthorSuggestions(searchQuery).collect { authors ->
                authorSuggestions = authors.take(3) // Limit to 3 suggestions
            }

            // Get book suggestions using fuzzy search
            val filteredBooks = FuzzySearchUtils
                .searchBooksSimple(allBooks, searchQuery, threshold = 0.25)
                .take(4) // Limit to 4 book suggestions
            bookSuggestions = filteredBooks

            showSuggestions = (authorSuggestions.isNotEmpty() || filteredBooks.isNotEmpty())
        } else {
            authorSuggestions = emptyList()
            bookSuggestions = emptyList()
            showSuggestions = false
        }
    }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && searchQuery.length >= 2) {
                            showSuggestions = true
                        }
                    },
                placeholder = {
                    Text(
                        "Search books, authors, or sagas...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { vm.clearSearch() }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    showSuggestions = false
                                    focusManager.clearFocus()
                                    onSearchExecuted()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Execute search",
                                tint = if (searchQuery.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Live suggestions
        if (showSuggestions && searchQuery.length >= 2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Author suggestions
                    if (authorSuggestions.isNotEmpty()) {
                        item {
                            SuggestionHeader(
                                title = "Authors",
                                icon = Icons.Default.Person
                            )
                        }
                        items(authorSuggestions) { author ->
                            AuthorSuggestionItem(
                                author = author,
                                onClick = {
                                    vm.updateSearchQuery(author)
                                    showSuggestions = false
                                    focusManager.clearFocus()
                                    onSearchExecuted()
                                }
                            )
                        }
                    }

                    // Book suggestions
                    if (bookSuggestions.isNotEmpty()) {
                        if (authorSuggestions.isNotEmpty()) {
                            item { Divider(modifier = Modifier.padding(vertical = 4.dp)) }
                        }
                        item {
                            SuggestionHeader(
                                title = "Books",
                                icon = Icons.Default.MenuBook
                            )
                        }
                        items(bookSuggestions) { book ->
                            BookSuggestionItem(
                                book = book,
                                searchQuery = searchQuery,
                                onClick = {
                                    vm.updateSearchQuery(book.title)
                                    showSuggestions = false
                                    focusManager.clearFocus()
                                    onSearchExecuted()
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
private fun SuggestionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuthorSuggestionItem(
    author: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BookSuggestionItem(
    book: Book,
    searchQuery: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = "by ${book.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.NorthEast,
                contentDescription = "Select",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}