package com.example.mybookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.ui.components.BookForm
import com.example.mybookhoard.ui.components.form.BookFormState
import com.example.mybookhoard.ui.components.form.hasChanges
import com.example.mybookhoard.ui.components.form.isValid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    // Estado inicial del formulario basado en el libro actual
    val initialFormState = remember(currentBook) {
        BookFormState(
            title = currentBook.title,
            author = currentBook.author ?: "",
            saga = currentBook.saga ?: "",
            description = currentBook.description ?: "",
            readingStatus = currentBook.status,
            wishlistStatus = currentBook.wishlist
        )
    }

    var formState by remember(initialFormState) { mutableStateOf(initialFormState) }

    // Estado del Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // Dialog de confirmación para cancelar
    var showCancelDialog by remember { mutableStateOf(false) }

    // Verificar si hay cambios
    val hasChanges = formState.hasChanges(initialFormState)

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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Edit \"${currentBook.title}\"",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Formulario
            Box(modifier = Modifier.weight(1f)) {
                BookForm(
                    formState = formState,
                    onFormStateChange = { formState = it },
                    vm = vm,
                    isEditMode = true
                )
            }

            // Botones de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        if (formState.isValid()) {
                            val updatedBook = currentBook.copy(
                                title = formState.title.trim(),
                                author = formState.author.trim().ifBlank { null },
                                saga = formState.saga.trim().ifBlank { null },
                                description = formState.description.trim().ifBlank { null },
                                status = formState.readingStatus,
                                wishlist = formState.wishlistStatus
                            )

                            try {
                                vm.updateBook(updatedBook)
                                snackbarMessage = "Book updated successfully!"
                                showSnackbar = true

                                // Navigate back after showing success message
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(1000)
                                    onNavigateBack()
                                }
                            } catch (e: Exception) {
                                snackbarMessage = "Error updating book: ${e.localizedMessage}"
                                showSnackbar = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = formState.isValid() && hasChanges
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