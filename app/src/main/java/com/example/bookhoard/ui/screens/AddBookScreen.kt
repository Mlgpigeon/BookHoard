package com.example.bookhoard.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookhoard.BooksVm
import com.example.bookhoard.data.Book
import com.example.bookhoard.data.WishlistStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(vm: BooksVm) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var saga by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var wishlistStatus by remember { mutableStateOf<WishlistStatus?>(null) }

    // Estado para las sugerencias de autores
    var authorSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAuthorSuggestions by remember { mutableStateOf(false) }

    // Estado para las sugerencias de sagas
    var sagaSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSagaSuggestions by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Buscar sugerencias de autores cuando el usuario escribe
    LaunchedEffect(author) {
        if (author.length >= 2) {
            vm.searchAuthorSuggestions(author).collect { suggestions ->
                authorSuggestions = suggestions
                showAuthorSuggestions = suggestions.isNotEmpty()
            }
        } else {
            authorSuggestions = emptyList()
            showAuthorSuggestions = false
        }
    }

    // Buscar sugerencias de sagas cuando el usuario escribe
    LaunchedEffect(saga) {
        if (saga.length >= 2) {
            vm.searchSagaSuggestions(saga).collect { suggestions ->
                sagaSuggestions = suggestions
                showSagaSuggestions = suggestions.isNotEmpty()
            }
        } else {
            sagaSuggestions = emptyList()
            showSagaSuggestions = false
        }
    }

    // Estado del Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add New Book",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                isError = title.isBlank(),
                supportingText = if (title.isBlank()) {
                    { Text("Title is required") }
                } else null
            )

            // Campo de autor con sugerencias
            AuthorField(
                author = author,
                onAuthorChange = {
                    author = it
                    if (it.isEmpty()) showAuthorSuggestions = false
                },
                suggestions = authorSuggestions,
                showSuggestions = showAuthorSuggestions,
                onSuggestionClick = {
                    author = it
                    showAuthorSuggestions = false
                }
            )

            // Campo de saga con sugerencias
            SagaField(
                saga = saga,
                onSagaChange = {
                    saga = it
                    if (it.isEmpty()) showSagaSuggestions = false
                },
                suggestions = sagaSuggestions,
                showSuggestions = showSagaSuggestions,
                onSuggestionClick = {
                    saga = it
                    showSagaSuggestions = false
                }
            )

            // Campo de descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Description",
                        tint = if (description.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                supportingText = { Text("Optional book description or summary") }
            )

            // Dropdown para wishlist
            WishlistSelector(
                wishlistStatus = wishlistStatus,
                onStatusChange = { wishlistStatus = it }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val book = Book(
                            title = title.trim(),
                            author = author.trim().ifBlank { null },
                            saga = saga.trim().ifBlank { null },
                            description = description.trim().ifBlank { null },
                            wishlist = wishlistStatus
                        )

                        try {
                            vm.addBook(book)
                            snackbarMessage = "Book '${book.title}' added successfully!"

                            // Limpiar campos
                            title = ""
                            author = ""
                            saga = ""
                            description = ""
                            wishlistStatus = null
                            showAuthorSuggestions = false
                            showSagaSuggestions = false
                        } catch (e: Exception) {
                            snackbarMessage = "Error adding book: ${e.localizedMessage}"
                        }

                        showSnackbar = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Add Book", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Mostrar Snackbar cuando se añade un libro
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }
}

@Composable
private fun AuthorField(
    author: String,
    onAuthorChange: (String) -> Unit,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = author,
            onValueChange = onAuthorChange,
            label = { Text("Author") },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Author",
                    tint = if (author.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            supportingText = if (showSuggestions && suggestions.isNotEmpty()) {
                { Text("${suggestions.size} suggestion${if (suggestions.size != 1) "s" else ""} found") }
            } else null
        )

        if (showSuggestions && suggestions.isNotEmpty()) {
            SuggestionCard(
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                icon = Icons.Default.Person,
                label = "Existing authors:"
            )
        }
    }
}

@Composable
private fun SagaField(
    saga: String,
    onSagaChange: (String) -> Unit,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = saga,
            onValueChange = onSagaChange,
            label = { Text("Series/Saga") },
            leadingIcon = {
                Icon(
                    Icons.Default.LibraryBooks,
                    contentDescription = "Saga",
                    tint = if (saga.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            supportingText = if (showSuggestions && suggestions.isNotEmpty()) {
                { Text("${suggestions.size} existing series found") }
            } else null
        )

        if (showSuggestions && suggestions.isNotEmpty()) {
            SuggestionCard(
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                icon = Icons.Default.LibraryBooks,
                label = "Existing series:"
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistSelector(
    wishlistStatus: WishlistStatus?,
    onStatusChange: (WishlistStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (wishlistStatus) {
                    WishlistStatus.WISH -> "⭐ Wish"
                    WishlistStatus.ON_THE_WAY -> "📦 On the way"
                    WishlistStatus.OBTAINED -> "📚 Obtained"
                    null -> "Select wishlist status (optional)"
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("No wishlist status") },
                onClick = {
                    onStatusChange(null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("⭐ Wish") },
                onClick = {
                    onStatusChange(WishlistStatus.WISH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("📦 On the way") },
                onClick = {
                    onStatusChange(WishlistStatus.ON_THE_WAY)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("📚 Obtained") },
                onClick = {
                    onStatusChange(WishlistStatus.OBTAINED)
                    expanded = false
                }
            )
        }
    }
}