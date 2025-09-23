package com.example.mybookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.ui.components.BookForm
import com.example.mybookhoard.ui.components.form.BookFormState
import com.example.mybookhoard.ui.components.form.isValid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(vm: BooksVm) {
    var formState by remember { mutableStateOf(BookFormState()) }

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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Add New Book",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Formulario
            Box(modifier = Modifier.weight(1f)) {
                BookForm(
                    formState = formState,
                    onFormStateChange = { formState = it },
                    vm = vm,
                    isEditMode = false
                )
            }

            // Botón de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (formState.isValid()) {
                            val book = Book(
                                title = formState.title.trim(),
                                author = formState.author.trim().ifBlank { null },
                                saga = formState.saga.trim().ifBlank { null },
                                description = formState.description.trim().ifBlank { null },
                                wishlist = formState.wishlistStatus
                            )

                            try {
                                vm.addBook(book)
                                snackbarMessage = "Book '${book.title}' added successfully!"

                                // Limpiar formulario
                                formState = BookFormState()
                            } catch (e: Exception) {
                                snackbarMessage = "Error adding book: ${e.localizedMessage}"
                            }

                            showSnackbar = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = formState.isValid()
                ) {
                    Text("Add Book", style = MaterialTheme.typography.titleMedium)
                }
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