package com.example.mybookhoard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus

data class BookFormState(
    val title: String = "",
    val author: String = "",
    val saga: String = "",
    val description: String = "",
    val readingStatus: ReadingStatus = ReadingStatus.NOT_STARTED,
    val wishlistStatus: WishlistStatus? = null
)

@Composable
private fun BookFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label + if (isRequired) " *" else "") },
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = label,
                tint = if (value.isNotEmpty())
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier,
        isError = isError,
        supportingText = if (isError && supportingText != null) {
            { Text(supportingText) }
        } else if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        minLines = minLines,
        maxLines = maxLines
    )
}

@Composable
private fun AuthorFieldWithSuggestions(
    author: String,
    onAuthorChange: (String) -> Unit,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        BookFormField(
            value = author,
            onValueChange = onAuthorChange,
            label = "Author",
            leadingIcon = Icons.Default.Person,
            modifier = Modifier.fillMaxWidth(),
            supportingText = if (showSuggestions && suggestions.isNotEmpty()) {
                "${suggestions.size} suggestion${if (suggestions.size != 1) "s" else ""} found"
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
private fun SagaFieldWithSuggestions(
    saga: String,
    onSagaChange: (String) -> Unit,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        BookFormField(
            value = saga,
            onValueChange = onSagaChange,
            label = "Series/Saga",
            leadingIcon = Icons.Default.LibraryBooks,
            modifier = Modifier.fillMaxWidth(),
            supportingText = if (showSuggestions && suggestions.isNotEmpty()) {
                "${suggestions.size} existing series found"
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
fun BookForm(
    formState: BookFormState,
    onFormStateChange: (BookFormState) -> Unit,
    vm: BooksVm,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false
) {
    // Estados para las sugerencias
    var authorSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAuthorSuggestions by remember { mutableStateOf(false) }
    var sagaSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSagaSuggestions by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Buscar sugerencias de autores cuando el usuario escribe
    LaunchedEffect(formState.author) {
        if (formState.author.length >= 2) {
            vm.searchAuthorSuggestions(formState.author).collect { suggestions ->
                authorSuggestions = suggestions.filter { it != formState.author }
                showAuthorSuggestions = authorSuggestions.isNotEmpty()
            }
        } else {
            authorSuggestions = emptyList()
            showAuthorSuggestions = false
        }
    }

    // Buscar sugerencias de sagas cuando el usuario escribe
    LaunchedEffect(formState.saga) {
        if (formState.saga.length >= 2) {
            vm.searchSagaSuggestions(formState.saga).collect { suggestions ->
                sagaSuggestions = suggestions.filter { it != formState.saga }
                showSagaSuggestions = sagaSuggestions.isNotEmpty()
            }
        } else {
            sagaSuggestions = emptyList()
            showSagaSuggestions = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo de título
        BookFormField(
            value = formState.title,
            onValueChange = {
                onFormStateChange(formState.copy(title = it))
            },
            label = "Title",
            leadingIcon = Icons.Default.Description,
            modifier = Modifier.fillMaxWidth(),
            isRequired = true,
            isError = formState.title.isBlank(),
            supportingText = if (formState.title.isBlank()) "Title is required" else null
        )

        // Campo de autor con sugerencias
        AuthorFieldWithSuggestions(
            author = formState.author,
            onAuthorChange = {
                onFormStateChange(formState.copy(author = it))
                if (it.isEmpty()) showAuthorSuggestions = false
            },
            suggestions = authorSuggestions,
            showSuggestions = showAuthorSuggestions,
            onSuggestionClick = {
                onFormStateChange(formState.copy(author = it))
                showAuthorSuggestions = false
            }
        )

        // Campo de saga con sugerencias
        SagaFieldWithSuggestions(
            saga = formState.saga,
            onSagaChange = {
                onFormStateChange(formState.copy(saga = it))
                if (it.isEmpty()) showSagaSuggestions = false
            },
            suggestions = sagaSuggestions,
            showSuggestions = showSagaSuggestions,
            onSuggestionClick = {
                onFormStateChange(formState.copy(saga = it))
                showSagaSuggestions = false
            }
        )

        // Campo de descripción
        BookFormField(
            value = formState.description,
            onValueChange = {
                onFormStateChange(formState.copy(description = it))
            },
            label = "Description",
            leadingIcon = Icons.Default.Description,
            modifier = Modifier.fillMaxWidth(),
            supportingText = "Optional book description or summary",
            minLines = 3,
            maxLines = 6
        )

        // Selector de estado de lectura (solo en modo edición)
        if (isEditMode) {
            ReadingStatusSelector(
                currentStatus = formState.readingStatus,
                onStatusChange = {
                    onFormStateChange(formState.copy(readingStatus = it))
                }
            )
        }

        // Selector de wishlist
        if (isEditMode) {
            WishlistChipSelector(
                wishlistStatus = formState.wishlistStatus,
                onStatusChange = {
                    onFormStateChange(formState.copy(wishlistStatus = it))
                }
            )
        } else {
            WishlistDropdownSelector(
                wishlistStatus = formState.wishlistStatus,
                onStatusChange = {
                    onFormStateChange(formState.copy(wishlistStatus = it))
                }
            )
        }
    }
}

fun BookFormState.isValid(): Boolean = title.isNotBlank()

fun BookFormState.hasChanges(original: BookFormState): Boolean {
    return title != original.title ||
            author != original.author ||
            saga != original.saga ||
            description != original.description ||
            readingStatus != original.readingStatus ||
            wishlistStatus != original.wishlistStatus
}