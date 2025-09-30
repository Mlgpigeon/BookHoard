package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybookhoard.components.common.LoadingIndicator
import com.example.mybookhoard.components.sagas.*
import com.example.mybookhoard.viewmodels.SagasViewModel

/**
 * Main screen for managing sagas/series
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SagasManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    sagasViewModel: SagasViewModel = viewModel()
) {
    val sagas by sagasViewModel.sagas.collectAsState()
    val isLoading by sagasViewModel.isLoading.collectAsState()
    val error by sagasViewModel.error.collectAsState()
    val uiState by sagasViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saga Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text("Create Saga") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && sagas.isEmpty() -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        message = "Loading sagas..."
                    )
                }

                sagas.isEmpty() -> {
                    EmptySagasState(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SagasHeader(sagasCount = sagas.size)
                        }

                        items(sagas) { saga ->
                            SagaCard(
                                saga = saga,
                                onEdit = { onNavigateToEditor(saga.id) },
                                onDelete = { showDeleteDialog = saga.id }
                            )
                        }

                        // Padding for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { sagaId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Saga") },
            text = { Text("Are you sure you want to delete this saga? Books will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        sagasViewModel.deleteSaga(sagaId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SagasViewModel.SagaUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                sagasViewModel.resetUiState()
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