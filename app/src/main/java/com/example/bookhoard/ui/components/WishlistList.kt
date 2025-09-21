package com.example.bookhoard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookhoard.BooksVm
import com.example.bookhoard.data.Book
import com.example.bookhoard.data.WishlistStatus

@Composable
fun LiveWishlistView(
    books: List<Book>,
    vm: BooksVm,
    searchQuery: String,
    onBookClick: (Book) -> Unit = {}
) {
    val wish = books.filter { it.wishlist == WishlistStatus.WISH }
    val onTheWay = books.filter { it.wishlist == WishlistStatus.ON_THE_WAY }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con info de wishlist
        item {
            WishlistHeaderCard(
                totalResults = books.size,
                searchQuery = searchQuery,
                wish = wish.size,
                onTheWay = onTheWay.size
            )
        }

        // Libros "On the way" (prioridad alta)
        if (onTheWay.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveWishlistSection(
                    title = "📦 On the way",
                    books = onTheWay,
                    vm = vm,
                    defaultExpanded = true,
                    highlight = searchQuery,
                    onBookClick = onBookClick
                )
            }
        }

        // Libros en wishlist
        if (wish.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveWishlistSection(
                    title = "⭐ Wish",
                    books = wish,
                    vm = vm,
                    defaultExpanded = searchQuery.isNotEmpty(),
                    highlight = searchQuery,
                    onBookClick = onBookClick
                )
            }
        }

        // Mensaje si no hay resultados
        if (books.isEmpty()) {
            item {
                Spacer(Modifier.height(32.dp))
                EmptyWishlistCard(
                    searchQuery = searchQuery,
                    onClearSearch = { vm.clearWishlistSearch() }
                )
            }
        }
    }
}

@Composable
fun LiveWishlistSection(
    title: String,
    books: List<Book>,
    vm: BooksVm,
    defaultExpanded: Boolean = false,
    highlight: String = "",
    onBookClick: (Book) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    // Auto-expandir si hay búsqueda activa
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
                        LiveWishlistRow(
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
fun LiveWishlistRow(
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
            // Título con highlighting
            Text(
                text = "• ${book.title}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlight.isNotEmpty() &&
                    book.title.contains(highlight, ignoreCase = true))
                    FontWeight.Bold else FontWeight.Normal
            )

            // Autor
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

            // Saga
            if (book.saga != null) {
                Text(
                    text = "Serie: ${book.saga}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = if (highlight.isNotEmpty() &&
                        book.saga.contains(highlight, ignoreCase = true))
                        FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Menú de acciones
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Change wishlist status",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("⭐ Wish") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.WISH)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📦 On the way") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.ON_THE_WAY)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📚 Obtained (move to collection)") },
                    onClick = {
                        vm.updateWishlist(book, WishlistStatus.OBTAINED)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("❌ Remove from wishlist") },
                    onClick = {
                        vm.updateWishlist(book, null)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}