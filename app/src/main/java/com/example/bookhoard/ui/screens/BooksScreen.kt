package com.example.bookhoard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookhoard.BooksVm
import com.example.bookhoard.data.Book
import com.example.bookhoard.ui.components.ViewModeSelector
import com.example.bookhoard.ui.components.LiveSearchBar
import com.example.bookhoard.ui.components.LiveBooksView
import com.example.bookhoard.ui.components.LiveAuthorsView

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