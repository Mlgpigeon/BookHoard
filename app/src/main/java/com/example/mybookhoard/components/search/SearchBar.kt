package com.example.mybookhoard.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search books, authors, genres...",
    enabled: Boolean = true
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { newQuery ->
                onQueryChange(newQuery)
                isExpanded = newQuery.isNotEmpty() && suggestions.isNotEmpty()
            },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            isExpanded = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                    keyboardController?.hide()
                    isExpanded = false
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        // Suggestions dropdown
        if (isExpanded && suggestions.isNotEmpty()) {
            SuggestionsList(
                suggestions = suggestions,
                onSuggestionSelected = { suggestion ->
                    onSuggestionSelected(suggestion)
                    onSearch()
                    keyboardController?.hide()
                    isExpanded = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            suggestions.take(5).forEach { suggestion -> // Limit to 5 suggestions
                TextButton(
                    onClick = { onSuggestionSelected(suggestion) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (suggestion != suggestions.take(5).last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}