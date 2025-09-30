package com.example.mybookhoard.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    sagasViewModel: SagasViewModel = viewModel()
) {
    val currentSaga by sagasViewModel.currentSaga.collectAsState()
    val sagaBooks by sagasViewModel.sagaBooks.collectAsState()
    val isLoading by sagasViewModel.isLoading.collectAsState()
    val error by sagasViewModel.error.collectAsState()
    val uiState by sagasViewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isEditing = sagaId != null

    // Initialize for editing
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Saga metadata form
            SagaMetadataForm(
                name = name,
                onNameChange = { name = it },
                description = description,
                onDescriptionChange = { description = it },
                isCompleted = isCompleted,
                onIsCompletedChange = { isCompleted = it }
            )

            HorizontalDivider()

            // Books editor with drag & drop
            SagaBooksEditor(
                books = sagaBooks,
                onRemoveBook = { bookId ->
                    sagasViewModel.removeBookFromSaga(bookId)
                },
                onAddBook = onNavigateToBookPicker,
                onReorder = { fromIndex, toIndex ->
                    sagasViewModel.reorderBooks(fromIndex, toIndex)
                }
            )

            // Save button
            Button(
                onClick = {
                    if (isEditing && sagaId != null) {
                        sagasViewModel.updateSaga(
                            sagaId = sagaId,
                            name = name,
                            description = description,
                            isCompleted = isCompleted
                        )
                    } else {
                        sagasViewModel.createSaga(
                            name = name,
                            description = description,
                            primaryAuthorId = sagaBooks.firstOrNull()?.book?.primaryAuthorId,
                            isCompleted = isCompleted
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && name.isNotBlank() && sagaBooks.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Updating..." else "Creating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Update Saga" else "Create Saga")
                }
            }
        }
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SagasViewModel.SagaUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                onSagaSaved()
            }
            is SagasViewModel.SagaUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> { /* No action needed */ }
        }
    }

    // Handle errors
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            sagasViewModel.clearError()
        }
    }
}