package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.components.form.BookImagesManager
import com.example.mybookhoard.data.entities.BookWithUserDataExtended

/**
 * Screen for editing book details including images
 * Path: app/src/main/java/com/example/mybookhoard/screens/EditBookScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(
    bookWithUserData: BookWithUserDataExtended,
    onNavigateBack: () -> Unit,
    onSaveBook: (
        title: String,
        description: String?,
        images: List<String>,
        coverSelected: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val book = bookWithUserData.book

    var title by remember { mutableStateOf(book.title) }
    var description by remember { mutableStateOf(book.description ?: "") }
    var images by remember { mutableStateOf(book.images ?: emptyList()) }
    var coverSelected by remember { mutableStateOf(book.coverSelected) }

    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasChanges = title != book.title ||
            description != (book.description ?: "") ||
            images.toList() != (book.images ?: emptyList()).toList() ||
            coverSelected != book.coverSelected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Book") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSaveBook(
                                title.trim(),
                                description.trim().ifBlank { null },
                                images,
                                coverSelected
                            )
                        },
                        enabled = title.isNotBlank() && hasChanges
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
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

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                supportingText = { Text("Optional book description") }
            )

            // Images manager
            BookImagesManager(
                images = images,
                coverImageUrl = coverSelected,
                onImagesChange = { images = it },
                onCoverChange = { coverSelected = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "Note: Changes will be saved when you tap the save icon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
            }
        )
    }
}