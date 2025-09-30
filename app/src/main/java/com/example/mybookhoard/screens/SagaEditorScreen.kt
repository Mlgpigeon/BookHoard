package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.components.sagas.SagaMetadataForm
import com.example.mybookhoard.components.sagas.SagaBooksEditor
import com.example.mybookhoard.viewmodels.SagasViewModel

/**
 * Screen for creating or editing a saga
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SagaEditorScreen(
    sagaId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToBookPicker: () -> Unit,
    onSagaSaved: () -> Unit,
    sagasViewModel: SagasViewModel
) {
    val currentSaga by sagasViewModel.currentSaga.collectAsState()
    val sagaBooks by sagasViewModel.sagaBooks.collectAsState()
    val isLoading by sagasViewModel.isLoading.collectAsState()
    val error by sagasViewModel.error.collectAsState()
    val uiState by sagasViewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val isEditing = sagaId != null

    // Initialize for editing or creating
    LaunchedEffect(sagaId) {
        if (sagaId != null) {
            sagasViewModel.startEditing(sagaId)
        } else {
            sagasViewModel.startCreating()
        }
    }

    // Update form when saga loads
    LaunchedEffect(currentSaga) {
        currentSaga?.let { saga ->
            name = saga.name
            description = saga.description ?: ""
            isCompleted = saga.isCompleted
        }
    }

    // Show error messages
    LaunchedEffect(error) {
        error?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Handle successful save
    LaunchedEffect(uiState) {
        if (uiState is SagasViewModel.SagaUiState.Success) {
            snackbarHostState.showSnackbar(
                message = if (isEditing) "Saga updated successfully" else "Saga created successfully",
                duration = SnackbarDuration.Short
            )
            onSagaSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Saga" else "Create Saga") },
                navigationIcon = {
                    IconButton(onClick = {
                        sagasViewModel.cancelEditing()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Saga metadata form (non-scrollable section)
            SagaMetadataForm(
                name = name,
                onNameChange = { name = it },
                description = description,
                onDescriptionChange = { description = it },
                isCompleted = isCompleted,
                onIsCompletedChange = { isCompleted = it }
            )

            HorizontalDivider()

            // Books editor with drag & drop (scrollable LazyColumn)
            // Give it weight to take remaining space
            SagaBooksEditor(
                books = sagaBooks,
                onRemoveBook = { bookId ->
                    sagasViewModel.removeBookFromSaga(bookId)
                },
                onAddBook = onNavigateToBookPicker,
                onReorder = { fromIndex, toIndex ->
                    sagasViewModel.reorderBooks(fromIndex, toIndex)
                },
                modifier = Modifier.weight(1f)
            )

            // Save button (fixed at bottom)
            Button(
                onClick = {
                    if (isEditing && sagaId != null) {
                        sagasViewModel.updateSaga(
                            sagaId = sagaId,
                            name = name,
                            description = description.ifBlank { null },
                            isCompleted = isCompleted
                        )
                    } else {
                        sagasViewModel.createSaga(
                            name = name,
                            description = description.ifBlank { null },
                            primaryAuthorId = null,
                            isCompleted = isCompleted
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && sagaBooks.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isEditing) "Update Saga" else "Create Saga")
            }

            // Helper text
            if (sagaBooks.isEmpty()) {
                Text(
                    text = "Add at least one book to create the saga",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}