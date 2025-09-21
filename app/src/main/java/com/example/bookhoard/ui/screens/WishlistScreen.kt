package com.example.bookhoard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
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
import com.example.bookhoard.ui.components.ViewModeSelector

@Composable
fun WishlistScreen(
    vm: BooksVm,
    onBookClick: (Book) -> Unit = {}
) {
    val filteredBooks by vm.filteredWishlistBooks.collectAsState(initial = emptyList())
    val searchQuery by vm.wishlistSearchQuery.collectAsState()
    var viewMode by remember { mutableStateOf("books") }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        // Barra de búsqueda integrada siempre visible
        LiveSearchBar(
            query = searchQuery,
            onQueryChange = vm::updateWishlistSearchQuery,
            placeholder = "Busca en tu wishlist...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ViewModeSelector(viewMode) { viewMode = it }
        Spacer(Modifier.height(12.dp))

        // Contenido que se filtra en tiempo real
        if (viewMode == "books") {
            LiveWishlistView(filteredBooks, vm, searchQuery, onBookClick)
        } else {
            LiveWishlistAuthorsView(filteredBooks, searchQuery)
        }
    }
}

@Composable
private fun LiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty())
                    MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LiveWishlistView(
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
private fun WishlistHeaderCard(
    totalResults: Int,
    searchQuery: String,
    wish: Int,
    onTheWay: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (searchQuery.isBlank()) {
            CardDefaults.elevatedCardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        }
    ) {
        if (searchQuery.isBlank()) {
            // Vista normal de estadísticas de wishlist
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Mi Wishlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalResults libro${if (totalResults != 1) "s" else ""} deseado${if (totalResults != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WishlistStatChip("Wish", wish)
                    WishlistStatChip("En camino", onTheWay)
                }
            }
        } else {
            // Vista de resultados de búsqueda
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$totalResults resultado${if (totalResults != 1) "s" else ""} en wishlist",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\"$searchQuery\" • Búsqueda inteligente activa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistStatChip(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveWishlistSection(
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
private fun LiveWishlistRow(
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

@Composable
private fun EmptyWishlistCard(
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            if (searchQuery.isNotBlank()) {
                Text(
                    text = "No se encontraron resultados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "No hay libros en tu wishlist que coincidan con \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                OutlinedButton(onClick = onClearSearch) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Limpiar búsqueda")
                }
            } else {
                Text(
                    text = "Tu wishlist está vacía",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Añade libros que quieras leer a tu lista de deseos desde la pantalla principal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LiveWishlistAuthorsView(
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
                LiveAuthorSection("📦 On the way", onTheWayBooks, searchQuery)
            }
        }

        if (wishBooks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LiveAuthorSection("⭐ Wish", wishBooks, searchQuery)
            }
        }

        // Mensaje si no hay resultados
        if (books.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Spacer(Modifier.height(16.dp))
                EmptyWishlistCard(
                    searchQuery = searchQuery,
                    onClearSearch = { /* vm.clearWishlistSearch() */ }
                )
            }
        }
    }
}

@Composable
private fun LiveAuthorSection(
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
                        LiveAuthorRow(author = author, items = items, highlight = searchQuery)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAuthorRow(
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