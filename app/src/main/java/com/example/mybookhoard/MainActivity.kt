package com.example.mybookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.api.AuthState
import com.example.mybookhoard.api.ConnectionState
import com.example.mybookhoard.ui.screens.*

class MainActivity : ComponentActivity() {
    private val vm: BooksVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Import initial data only if not authenticated
        vm.importFromAssetsOnce(this)

        setContent {
            MaterialTheme {
                AppContent(vm)
            }
        }
    }
}

@Composable
fun AppContent(vm: BooksVm) {
    val authState by vm.authState.collectAsState()
    val connectionState by vm.connectionState.collectAsState()

    // Create local variable for smart cast
    val currentAuthState = authState

    when (currentAuthState) {
        is AuthState.NotAuthenticated, is AuthState.Error -> {
            AuthScreen(
                authState = currentAuthState,
                onLogin = { identifier, password ->
                    vm.login(identifier, password)
                },
                onRegister = { username, email, password ->
                    vm.register(username, email, password)
                },
                onClearError = {
                    vm.clearAuthError()
                },
                onRetry = {
                    vm.retryNetworkOperation()
                }
            )
        }

        is AuthState.Authenticating -> {
            LoadingScreen(message = "Signing in...")
        }

        is AuthState.Authenticated -> {
            MainAppScreen(vm, currentAuthState, connectionState)
        }
    }
}

@Composable
fun LoadingScreen(message: String = "Loading...") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )

            // Add a subtle hint about network activity
            Text(
                text = "Please wait while we connect to the server...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

sealed class Screen {
    object Books : Screen()
    object Add : Screen()
    object Wishlist : Screen()
    object Sync : Screen()
    object Profile : Screen()
    data class BookDetail(val bookId: Long) : Screen()
    data class EditBook(val bookId: Long) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    vm: BooksVm,
    authState: AuthState.Authenticated,
    connectionState: ConnectionState
) {
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
                    TopAppBar(
                        title = { Text("BookHoard") },
                        actions = {
                            // Connection status indicator
                            ConnectionIndicator(
                                connectionState = connectionState,
                                onRetry = { vm.retryNetworkOperation() }
                            )
                        }
                    )
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
                            CloudSyncScreen(
                                vm = vm,
                                authState = authState,
                                connectionState = connectionState
                            )
                        }

                        is Screen.BookDetail, is Screen.EditBook -> {
                            // These cases are handled above
                        }

                        Screen.Profile -> {
                            ProfileScreen(
                                vm = vm,
                                user = authState.user,
                                onNavigateBack = { currentScreen = Screen.Books }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionIndicator(
    connectionState: ConnectionState,
    onRetry: () -> Unit
) {
    when (connectionState) {
        is ConnectionState.Online -> {
            Icon(
                Icons.Default.CloudSync,
                contentDescription = "Online",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        is ConnectionState.Offline -> {
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = "Offline - Tap to retry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        is ConnectionState.Syncing -> {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
        is ConnectionState.Error -> {
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = "Sync Error - Tap to retry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}