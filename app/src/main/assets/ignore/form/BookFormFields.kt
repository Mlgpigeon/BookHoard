package com.example.mybookhoard.ui.components.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun BookFormField(
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
fun AuthorFieldWithSuggestions(
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
fun SagaFieldWithSuggestions(
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