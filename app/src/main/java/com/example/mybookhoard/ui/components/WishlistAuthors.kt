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
import com.example.mybookhoard.data.WishlistStatus

@Composable
fun LiveWishlistAuthorsView(
    books: List<Book>,
    searchQuery: String
) {
    val wishBooks = books.filter { it.wishlist == WishlistStatus.WISH }
    val onTheWayBooks = books.filter { it.wishlist == WishlistStatus.ON_THE_WAY }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            WishlistHeaderCard(
                totalResults = books.size,
                searchQuery = searchQuery,
                wish = wishBooks.size,
                onTheWay = onTheWayBooks.size
            )
        }

        // Secciones por estado
        if (onTheWayBooks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveWishlistAuthorSection("📦 On the way", onTheWayBooks, searchQuery)
            }
        }

        if (wishBooks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveWishlistAuthorSection("⭐ Wish", wishBooks, searchQuery)
            }
        }

        // Mensaje si no hay resultados
        if (books.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Spacer(Modifier.height(16.dp))
                EmptyWishlistCard(
                    searchQuery = searchQuery,
                    onClearSearch = { }
                )
            }
        }
    }
}

@Composable
fun LiveWishlistAuthorSection(
    title: String,
    books: List<Book>,
    searchQuery: String
) {
    var expanded by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    val byAuthor = books
        .groupBy { it.author?.ifBlank { "Unknown" } ?: "Unknown" }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    // Auto-expandir si hay búsqueda activa
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
                        LiveWishlistAuthorRow(author = author, items = items, highlight = searchQuery)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWishlistAuthorRow(
    author: String,
    items: List<Book>,
    highlight: String = ""
) {
    var open by remember { mutableStateOf(highlight.isNotEmpty()) }

    // Auto-expandir si el autor coincide con la búsqueda
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
                    color = MaterialTheme.colorScheme.secondary
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