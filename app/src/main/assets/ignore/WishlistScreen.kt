package com.example.mybookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.ui.components.ViewModeSelector
import com.example.mybookhoard.ui.components.LiveSearchBar
import com.example.mybookhoard.ui.components.LiveWishlistView
import com.example.mybookhoard.ui.components.LiveWishlistAuthorsView

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