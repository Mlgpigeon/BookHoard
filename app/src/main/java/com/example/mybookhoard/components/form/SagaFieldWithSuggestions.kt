package com.example.mybookhoard.components.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.api.books.SagaSuggestion

/**
 * Saga field with suggestions and number input
 * Path: app/src/main/java/com/example/mybookhoard/components/form/SagaFieldWithSuggestions.kt
 */
@Composable
fun SagaFieldWithSuggestions(
    sagaName: String,
    onSagaNameChange: (String) -> Unit,
    sagaNumber: String,
    onSagaNumberChange: (String) -> Unit,
    suggestions: List<SagaSuggestion>,
    onSuggestionClick: (SagaSuggestion) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Saga name field
        OutlinedTextField(
            value = sagaName,
            onValueChange = onSagaNameChange,
            label = { Text("Saga / Series") },
            leadingIcon = {
                Icon(
                    Icons.Default.LibraryBooks,
                    contentDescription = "Saga",
                    tint = if (sagaName.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (sagaName.isNotEmpty()) {
                {
                    IconButton(onClick = { onSagaNameChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            singleLine = true,
            supportingText = {
                if (isError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (suggestions.isNotEmpty()) {
                    Text("${suggestions.size} existing saga${if (suggestions.size != 1) "s" else ""} found")
                } else {
                    Text("Optional: Add book to a saga/series")
                }
            }
        )

        // Saga suggestions
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LibraryBooks,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Existing sagas:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    suggestions.take(5).forEach { suggestion ->
                        SagaSuggestionItem(
                            suggestion = suggestion,
                            onClick = { onSuggestionClick(suggestion) }
                        )
                    }
                }
            }
        }

        // Saga number field (only show if saga name is filled)
        if (sagaName.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sagaNumber,
                onValueChange = onSagaNumberChange,
                label = { Text("Book Number in Saga") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Numbers,
                        contentDescription = "Number",
                        tint = if (sagaNumber.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Optional: Position in the saga (e.g., 1, 2, 3...)") }
            )
        }
    }
}

@Composable
private fun SagaSuggestionItem(
    suggestion: SagaSuggestion,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${suggestion.totalBooks} book${if (suggestion.totalBooks != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (suggestion.isCompleted) {
                        Text(
                            text = "• Completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}