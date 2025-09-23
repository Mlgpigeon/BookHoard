package com.example.mybookhoard.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus

@Composable
fun LiveBooksView(
    books: List<Book>,
    vm: BooksVm,
    searchQuery: String,
    onBookClick: (Book) -> Unit = {}
) {
    val reading = books.filter { it.status == ReadingStatus.READING }
    val unread = books.filter { it.status == ReadingStatus.NOT_STARTED }
    val read = books.filter { it.status == ReadingStatus.READ }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con estadísticas o info de búsqueda
        item {
            SearchHeaderCard(
                totalResults = books.size,
                searchQuery = searchQuery,
                reading = reading.size,
                unread = unread.size,
                read = read.size
            )
        }

        // Libros que estás leyendo (siempre visibles si hay)
        if (reading.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveSection(
                    title = "📖 Reading Now",
                    books = reading,
                    vm = vm,
                    defaultExpanded = true,
                    highlight = searchQuery,
                    onBookClick = onBookClick
                )
            }
        }

        // Libros sin leer
        if (unread.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveSection(
                    title = "📕 Unread",
                    books = unread,
                    vm = vm,
                    defaultExpanded = searchQuery.isNotEmpty(),
                    highlight = searchQuery,
                    onBookClick = onBookClick
                )
            }
        }

        // Libros leídos
        if (read.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveSection(
                    title = "✅ Read",
                    books = read,
                    vm = vm,
                    defaultExpanded = searchQuery.isNotEmpty(),
                    highlight = searchQuery,
                    onBookClick = onBookClick
                )
            }
        }

        // Mensaje si no hay resultados
        if (books.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Spacer(Modifier.height(32.dp))
                EmptySearchResultCard(
                    searchQuery = searchQuery,
                    onClearSearch = { vm.clearSearch() }
                )
            }
        }
    }
}

@Composable
fun LiveSection(
    title: String,
    books: List<Book>,
    vm: BooksVm,
    defaultExpanded: Boolean = false,
    highlight: String = "",
    onBookClick: (Book) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    LaunchedEffect(highlight) {
        if (highlight.isNotEmpty()) {
            expanded = true
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title (${books.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (books.isNotEmpty()) {
                    TextButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Text(
                            if (expanded) "Ocultar" else "Mostrar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded && books.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    books.forEach { book ->
                        LiveBookRow(
                            book = book,
                            vm = vm,
                            highlight = highlight,
                            onBookClick = onBookClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveBookRow(
    book: Book,
    vm: BooksVm,
    highlight: String = "",
    onBookClick: (Book) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick(book) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable { onBookClick(book) }
        ) {
            Text(
                text = "• ${book.title}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlight.isNotEmpty() &&
                    book.title.contains(highlight, ignoreCase = true))
                    FontWeight.Bold else FontWeight.Normal
            )

            if (book.author != null) {
                Text(
                    text = "por ${book.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (highlight.isNotEmpty() &&
                        book.author.contains(highlight, ignoreCase = true))
                        FontWeight.Bold else FontWeight.Normal
                )
            }

            if (book.saga != null) {
                Text(
                    text = "Serie: ${book.saga}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = if (highlight.isNotEmpty() &&
                        book.saga.contains(highlight, ignoreCase = true))
                        FontWeight.Bold else FontWeight.Normal
                )
            }

            // Show wishlist status if present
            if (book.wishlist != null) {
                Text(
                    text = when (book.wishlist) {
                        WishlistStatus.WISH -> "⭐ In Wishlist"
                        WishlistStatus.ON_THE_WAY -> "📦 On the Way"
                        WishlistStatus.OBTAINED -> "📚 Obtained"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Change status",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                // Reading Status Section
                Text(
                    text = "Reading Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                DropdownMenuItem(
                    text = { Text("📕 Unread") },
                    onClick = {
                        vm.updateStatus(book, ReadingStatus.NOT_STARTED)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📖 Reading") },
                    onClick = {
                        vm.updateStatus(book, ReadingStatus.READING)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("✅ Read") },
                    onClick = {
                        vm.updateStatus(book, ReadingStatus.READ)
                        menuExpanded = false
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Wishlist Section
                Text(
                    text = "Wishlist",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.secondary
                )

                DropdownMenuItem(
                    text = { Text("⭐ Add to Wishlist") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.WISH)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📦 Mark as On the Way") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.ON_THE_WAY)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📚 Mark as Obtained") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.OBTAINED)
                        menuExpanded = false
                    }
                )

                if (book.wishlist != null) {
                    DropdownMenuItem(
                        text = { Text("❌ Remove from Wishlist") },
                        onClick = {
                            vm.updateWishlist(book, null)
                            menuExpanded = false
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Delete Section
                DropdownMenuItem(
                    text = {
                        Text(
                            "🗑️ Delete Book",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        vm.deleteBook(book)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}