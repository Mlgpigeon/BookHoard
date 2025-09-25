package com.example.mybookhoard.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus

@Composable
fun LiveAuthorsView(
    books: List<Book>,
    searchQuery: String
) {
    val sections = listOf(
        "📖 Reading" to ReadingStatus.READING,
        "📕 Unread" to ReadingStatus.NOT_STARTED,
        "✅ Read" to ReadingStatus.READ
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SearchHeaderCard(
                totalResults = books.size,
                searchQuery = searchQuery,
                reading = books.count { it.status == ReadingStatus.READING },
                unread = books.count { it.status == ReadingStatus.NOT_STARTED },
                read = books.count { it.status == ReadingStatus.READ }
            )
        }

        sections.forEach { (label, status) ->
            val bucket = books.filter { it.status == status }
            if (bucket.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    LiveAuthorSection(label, bucket, searchQuery)
                }
            }
        }

        if (books.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Spacer(Modifier.height(16.dp))
                EmptySearchResultCard(
                    searchQuery = searchQuery,
                    onClearSearch = { }
                )
            }
        }
    }
}

@Composable
fun LiveAuthorSection(
    title: String,
    books: List<Book>,
    searchQuery: String
) {
    var expanded by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    val byAuthor = books
        .groupBy { it.author?.ifBlank { "Unknown" } ?: "Unknown" }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            expanded = true
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title (${books.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Ocultar" else "Mostrar")
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    byAuthor.forEach { (author, items) ->
                        LiveAuthorRow(author = author, items = items, highlight = searchQuery)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveAuthorRow(
    author: String,
    items: List<Book>,
    highlight: String = ""
) {
    var open by remember { mutableStateOf(highlight.isNotEmpty()) }

    LaunchedEffect(highlight) {
        if (highlight.isNotEmpty() && author.contains(highlight, ignoreCase = true)) {
            open = true
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = author,
                fontWeight = if (highlight.isNotEmpty() &&
                    author.contains(highlight, ignoreCase = true))
                    FontWeight.Bold else FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (open) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                items.forEach { book ->
                    Text(
                        text = "• ${book.title}",
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (highlight.isNotEmpty() &&
                            book.title.contains(highlight, ignoreCase = true))
                            FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}