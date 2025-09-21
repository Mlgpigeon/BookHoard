package com.example.bookhoard.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
fun AddBookScreen(vm: BooksVm) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var saga by remember { mutableStateOf("") }
    var wishlistStatus by remember { mutableStateOf<WishlistStatus?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }

    // Estado para las sugerencias de autores
    var authorSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAuthorSuggestions by remember { mutableStateOf(false) }

    // Buscar sugerencias de autores cuando el usuario escribe
    LaunchedEffect(author) {
        if (author.length >= 2) {
            vm.searchAuthorSuggestions(author).collect { suggestions ->
                authorSuggestions = suggestions
                showAuthorSuggestions = suggestions.isNotEmpty()
            }
        } else {
            authorSuggestions = emptyList()
            showAuthorSuggestions = false
        }
    }

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Add New Book",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            modifier = Modifier.fillMaxWidth(),
            isError = title.isBlank(),
            supportingText = if (title.isBlank()) {
                { Text("Title is required") }
            } else null
        )

        // Campo de autor con sugerencias
        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = author,
                onValueChange = {
                    author = it
                    // Si el usuario borra todo, ocultar sugerencias
                    if (it.isEmpty()) {
                        showAuthorSuggestions = false
                    }
                },
                label = { Text("Author") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Author",
                        tint = if (author.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (showAuthorSuggestions && authorSuggestions.isNotEmpty()) {
                    { Text("${authorSuggestions.size} suggestion${if (authorSuggestions.size != 1) "s" else ""} found") }
                } else null
            )

            // Lista de sugerencias de autores
            if (showAuthorSuggestions && authorSuggestions.isNotEmpty()) {
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
                            text = "Existing authors:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        authorSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        author = suggestion
                                        showAuthorSuggestions = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
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
        }

        OutlinedTextField(
            value = saga,
            onValueChange = { saga = it },
            label = { Text("Series/Saga") },
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown para wishlist
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (wishlistStatus) {
                        WishlistStatus.WISH -> "⭐ Wish"
                        WishlistStatus.ON_THE_WAY -> "📦 On the way"
                        WishlistStatus.OBTAINED -> "📚 Obtained"
                        null -> "Select wishlist status (optional)"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No wishlist status") },
                    onClick = {
                        wishlistStatus = null
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("⭐ Wish") },
                    onClick = {
                        wishlistStatus = WishlistStatus.WISH
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📦 On the way") },
                    onClick = {
                        wishlistStatus = WishlistStatus.ON_THE_WAY
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📚 Obtained") },
                    onClick = {
                        wishlistStatus = WishlistStatus.OBTAINED
                        expanded = false
                    }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    val book = Book(
                        title = title.trim(),
                        author = author.trim().ifBlank { null },
                        saga = saga.trim().ifBlank { null },
                        wishlist = wishlistStatus
                    )
                    vm.addBook(book)

                    // Limpiar campos
                    title = ""
                    author = ""
                    saga = ""
                    wishlistStatus = null
                    showAuthorSuggestions = false

                    showSnackbar = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = title.isNotBlank()
        ) {
            Text("Add Book", style = MaterialTheme.typography.titleMedium)
        }
    }

    // Snackbar para confirmación
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }
    }
}