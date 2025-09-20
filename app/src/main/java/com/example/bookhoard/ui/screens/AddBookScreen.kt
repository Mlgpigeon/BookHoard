package com.example.bookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("Author") },
            modifier = Modifier.fillMaxWidth()
        )

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