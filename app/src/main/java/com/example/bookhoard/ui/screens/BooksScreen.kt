package com.example.bookhoard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookhoard.BooksVm
import com.example.bookhoard.data.Book
import com.example.bookhoard.data.ReadingStatus
import com.example.bookhoard.ui.components.ViewModeSelector
import com.example.bookhoard.data.WishlistStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    vm: BooksVm,
    onBookClick: (Book) -> Unit = {}
) {
    val filteredBooks by vm.filteredBooks.collectAsState(initial = emptyList())
    val searchQuery by vm.searchQuery.collectAsState()
    var viewMode by remember { mutableStateOf("books") }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        // Barra de búsqueda integrada siempre visible
        LiveSearchBar(
            query = searchQuery,
            onQueryChange = vm::updateSearchQuery,
            placeholder = "Busca libros, autores, sagas...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ViewModeSelector(viewMode) { viewMode = it }
        Spacer(Modifier.height(12.dp))

        // Contenido que se filtra en tiempo real
        if (viewMode == "books") {
            LiveBooksView(filteredBooks, vm, searchQuery, onBookClick)
        } else {
            LiveAuthorsView(filteredBooks, searchQuery)
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
                    MaterialTheme.colorScheme.primary
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
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LiveBooksView(
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
private fun SearchHeaderCard(
    totalResults: Int,
    searchQuery: String,
    reading: Int,
    unread: Int,
    read: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (searchQuery.isBlank()) {
            CardDefaults.elevatedCardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        }
    ) {
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip("Total", totalResults, Modifier.weight(1f))
                StatChip("Reading", reading, Modifier.weight(1f))
                StatChip("Unread", unread, Modifier.weight(1f))
                StatChip("Read", read, Modifier.weight(1f))
            }
        } else {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$totalResults resultado${if (totalResults != 1) "s" else ""}",
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
private fun StatChip(
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveSection(
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
private fun LiveBookRow(
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
            }
        }
    }
}

@Composable
private fun EmptySearchResultCard(
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
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "No se encontraron resultados",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "No hay libros que coincidan con \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = onClearSearch
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Limpiar búsqueda")
            }
        }
    }
}

@Composable
private fun LiveAuthorsView(
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
private fun LiveAuthorSection(
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
private fun LiveAuthorRow(
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