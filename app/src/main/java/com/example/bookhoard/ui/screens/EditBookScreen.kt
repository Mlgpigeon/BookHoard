package com.example.bookhoard.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.bookhoard.data.ReadingStatus
import com.example.bookhoard.data.WishlistStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(
    bookId: Long,
    vm: BooksVm,
    onNavigateBack: () -> Unit
) {
    val book by vm.getBookById(bookId).collectAsState(initial = null)

    if (book == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val currentBook = book!!

    var title by remember { mutableStateOf(currentBook.title) }
    var author by remember { mutableStateOf(currentBook.author ?: "") }
    var saga by remember { mutableStateOf(currentBook.saga ?: "") }
    var description by remember { mutableStateOf(currentBook.description ?: "") }
    var readingStatus by remember { mutableStateOf(currentBook.status) }
    var wishlistStatus by remember { mutableStateOf(currentBook.wishlist) }

    // Estado para las sugerencias
    var authorSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAuthorSuggestions by remember { mutableStateOf(false) }
    var sagaSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSagaSuggestions by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Buscar sugerencias de autores
    LaunchedEffect(author) {
        if (author.length >= 2) {
            vm.searchAuthorSuggestions(author).collect { suggestions ->
                authorSuggestions = suggestions.filter { it != author }
                showAuthorSuggestions = authorSuggestions.isNotEmpty()
            }
        } else {
            authorSuggestions = emptyList()
            showAuthorSuggestions = false
        }
    }

    // Buscar sugerencias de sagas
    LaunchedEffect(saga) {
        if (saga.length >= 2) {
            vm.searchSagaSuggestions(saga).collect { suggestions ->
                sagaSuggestions = suggestions.filter { it != saga }
                showSagaSuggestions = sagaSuggestions.isNotEmpty()
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

    // Dialog de confirmación para cancelar
    var showCancelDialog by remember { mutableStateOf(false) }

    // Verificar si hay cambios
    val hasChanges = title != currentBook.title ||
            author != (currentBook.author ?: "") ||
            saga != (currentBook.saga ?: "") ||
            description != (currentBook.description ?: "") ||
            readingStatus != currentBook.status ||
            wishlistStatus != currentBook.wishlist

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Book") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showCancelDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
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
                text = "Edit \"${currentBook.title}\"",
                style = MaterialTheme.typography.headlineSmall,
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
            EditAuthorField(
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
            EditSagaField(
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

            // Selector de estado de lectura
            ReadingStatusEditSelector(
                currentStatus = readingStatus,
                onStatusChange = { readingStatus = it }
            )

            // Selector de wishlist
            WishlistEditSelector(
                wishlistStatus = wishlistStatus,
                onStatusChange = { wishlistStatus = it }
            )

            Spacer(Modifier.weight(1f))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (hasChanges) {
                            showCancelDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val updatedBook = currentBook.copy(
                                title = title.trim(),
                                author = author.trim().ifBlank { null },
                                saga = saga.trim().ifBlank { null },
                                description = description.trim().ifBlank { null },
                                status = readingStatus,
                                wishlist = wishlistStatus
                            )

                            try {
                                vm.updateBook(updatedBook)
                                snackbarMessage = "Book updated successfully!"
                                showSnackbar = true

                                // Navigate back after showing success message
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    kotlinx.coroutines.delay(1000)
                                    onNavigateBack()
                                }
                            } catch (e: Exception) {
                                snackbarMessage = "Error updating book: ${e.localizedMessage}"
                                showSnackbar = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() && hasChanges
                ) {
                    Text("Save Changes")
                }
            }
        }
    }

    // Dialog de confirmación para cancelar
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back without saving?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Continue Editing")
                }
            }
        )
    }

    // Mostrar Snackbar
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
private fun EditAuthorField(
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
            EditSuggestionCard(
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                icon = Icons.Default.Person,
                label = "Existing authors:"
            )
        }
    }
}

@Composable
private fun EditSagaField(
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
            EditSuggestionCard(
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                icon = Icons.Default.LibraryBooks,
                label = "Existing series:"
            )
        }
    }
}

@Composable
private fun EditSuggestionCard(
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
private fun ReadingStatusEditSelector(
    currentStatus: ReadingStatus,
    onStatusChange: (ReadingStatus) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Reading Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadingStatus.values().forEach { status ->
                val isSelected = currentStatus == status
                val (label, color) = when (status) {
                    ReadingStatus.NOT_STARTED -> "Unread" to MaterialTheme.colorScheme.outline
                    ReadingStatus.READING -> "Reading" to MaterialTheme.colorScheme.primary
                    ReadingStatus.READ -> "Read" to MaterialTheme.colorScheme.tertiary
                }

                FilterChip(
                    onClick = { onStatusChange(status) },
                    label = { Text(label) },
                    selected = isSelected,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor = color
                    )
                )
            }
        }
    }
}

@Composable
private fun WishlistEditSelector(
    wishlistStatus: WishlistStatus?,
    onStatusChange: (WishlistStatus?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Wishlist Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val options = listOf(
            null to "None",
            WishlistStatus.WISH to "⭐ Wish",
            WishlistStatus.ON_THE_WAY to "📦 On the way",
            WishlistStatus.OBTAINED to "📚 Obtained"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (status, label) ->
                val isSelected = wishlistStatus == status

                FilterChip(
                    onClick = { onStatusChange(status) },
                    label = { Text(label) },
                    selected = isSelected,
                    modifier = Modifier.fillMaxWidth(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
    }
}