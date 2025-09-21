package com.example.bookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookhoard.ui.screens.AddBookScreen
import com.example.bookhoard.BooksVm
import com.example.bookhoard.ui.screens.BooksScreen
import com.example.bookhoard.ui.screens.WishlistScreen
import com.example.bookhoard.ui.screens.BookDetailScreen
import com.example.bookhoard.ui.screens.EditBookScreen
import com.example.bookhoard.ui.screens.SyncScreen
import com.example.bookhoard.data.Book

class MainActivity : ComponentActivity() {
    private val vm: BooksVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.importFromAssetsOnce(this)
        setContent {
            MaterialTheme {
                MainScreen(vm)
            }
        }
    }
}

sealed class Screen {
    object Books : Screen()
    object Add : Screen()
    object Wishlist : Screen()
    object Sync : Screen()
    data class BookDetail(val bookId: Long) : Screen()
    data class EditBook(val bookId: Long) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: BooksVm) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Books) }

    // Store current screen value to enable smart cast
    val screen = currentScreen

    when (screen) {
        is Screen.BookDetail -> {
            BookDetailScreen(
                bookId = screen.bookId,
                vm = vm,
                onNavigateBack = { currentScreen = Screen.Books },
                onEditBook = { book ->
                    currentScreen = Screen.EditBook(book.id)
                }
            )
        }
        is Screen.EditBook -> {
            EditBookScreen(
                bookId = screen.bookId,
                vm = vm,
                onNavigateBack = { currentScreen = Screen.Books }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("BookHoard") })
                },
                bottomBar = {
                    BottomAppBar {
                        NavigationBar {
                            NavigationBarItem(
                                selected = screen == Screen.Books,
                                onClick = { currentScreen = Screen.Books },
                                icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Books") },
                                label = { Text("Books") }
                            )
                            NavigationBarItem(
                                selected = screen == Screen.Add,
                                onClick = { currentScreen = Screen.Add },
                                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                                label = { Text("Add") }
                            )
                            NavigationBarItem(
                                selected = screen == Screen.Wishlist,
                                onClick = { currentScreen = Screen.Wishlist },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Wishlist") },
                                label = { Text("Wishlist") }
                            )
                            NavigationBarItem(
                                selected = screen == Screen.Sync,
                                onClick = { currentScreen = Screen.Sync },
                                icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sync") },
                                label = { Text("Sync") }
                            )
                        }
                    }
                }
            ) { paddings ->
                Box(Modifier.padding(paddings)) {
                    when (screen) {
                        Screen.Books -> BooksScreen(
                            vm = vm,
                            onBookClick = { book -> currentScreen = Screen.BookDetail(book.id) }
                        )
                        Screen.Add -> AddBookScreen(vm)
                        Screen.Wishlist -> WishlistScreen(
                            vm = vm,
                            onBookClick = { book -> currentScreen = Screen.BookDetail(book.id) }
                        )
                        Screen.Sync -> {
                            SyncScreen(vm = vm, googleDriveSync = vm.googleDriveSync)
                        }

                        is Screen.BookDetail, is Screen.EditBook -> {
                            // These cases are handled above
                        }
                    }
                }
            }
        }
    }
}