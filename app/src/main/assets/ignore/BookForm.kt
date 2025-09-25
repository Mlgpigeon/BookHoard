package com.example.mybookhoard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.ui.components.form.*

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