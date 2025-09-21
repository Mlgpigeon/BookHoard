package com.example.bookhoard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookhoard.BooksVm
import com.example.bookhoard.data.Book
import com.example.bookhoard.data.ReadingStatus
import com.example.bookhoard.data.WishlistStatus

@Composable
fun ViewModeSelector(viewMode: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { onChange("books") },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (viewMode == "books")
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
        ) {
            Text("Books")
        }
        OutlinedButton(
            onClick = { onChange("authors") },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (viewMode == "authors")
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
        ) {
            Text("Authors")
        }
    }
}

@Composable
fun BookRow(
    book: Book,
    vm: BooksVm,
    onBookClick: (Book) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onBookClick(book) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable { onBookClick(book) }
        ) {
            Text("• ${book.title}")
            if (book.author != null) {
                Text(
                    text = "by ${book.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (book.saga != null) {
                Text(
                    text = "Series: ${book.saga}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true }
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Change status"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
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
            }
        }
    }
}

@Composable
fun WishlistRow(
    book: Book,
    vm: BooksVm,
    onBookClick: (Book) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onBookClick(book) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable { onBookClick(book) }
        ) {
            Text("• ${book.title}")
            if (book.author != null) {
                Text(
                    text = "by ${book.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (book.saga != null) {
                Text(
                    text = "Series: ${book.saga}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Star, contentDescription = "Change wishlist status")
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
                    text = { Text("Remove from wishlist") },
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
fun AuthorsView(books: List<Book>) {
    val sections = listOf(
        "Read" to ReadingStatus.READ,
        "Reading" to ReadingStatus.READING,
        "Unread" to ReadingStatus.NOT_STARTED
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { (label, status) ->
            val bucket = books.filter { it.status == status }
            if (bucket.isNotEmpty()) {
                item { StatusSection(label, bucket) }
            }
        }
    }
}

@Composable
private fun StatusSection(title: String, books: List<Book>) {
    var expanded by remember { mutableStateOf(true) }
    val byAuthor = books
        .groupBy { it.author?.ifBlank { "Unknown" } ?: "Unknown" }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$title (${books.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                byAuthor.forEach { (author, items) ->
                    AuthorRow(author = author, items = items)
                }
            }
        }
    }
}

@Composable
private fun AuthorRow(author: String, items: List<Book>) {
    var open by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(author, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(items.size.toString())
        }
        if (open) {
            Spacer(Modifier.height(2.dp))
            items.forEach { book ->
                Text(
                    text = "• ${book.title}",
                    modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}