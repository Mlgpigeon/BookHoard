package com.example.mybookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.api.AuthState
import com.example.mybookhoard.api.ConnectionState
import com.example.mybookhoard.ui.screens.*

class MainActivity : ComponentActivity() {
    private val vm: BooksVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    val context = LocalContext.current

    // Import initial data only if not authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.NotAuthenticated) {
            vm.importFromAssetsOnce(context)
        }
    }

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
    object Library : Screen()
    object Add : Screen()
    object Sync : Screen()
    object Profile : Screen()
    object Settings : Screen()  // AÑADIR ESTA LÍNEA
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

    // Store current screen value to enable smart cast
    val screen = currentScreen

    when (screen) {
        is Screen.BookDetail -> {
            BookDetailScreen(
                bookId = screen.bookId,
                vm = vm,
                onNavigateBack = { currentScreen = Screen.Library },
                onEditBook = { book ->
                    currentScreen = Screen.EditBook(book.id)
                }
            )
        }
        is Screen.EditBook -> {
            EditBookScreen(
                bookId = screen.bookId,
                vm = vm,
                onNavigateBack = { currentScreen = Screen.Library }
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
                                selected = screen == Screen.Library,
                                onClick = { currentScreen = Screen.Library },
                                icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Library") },
                                label = { Text("Library") }
                            )
                            NavigationBarItem(
                                selected = screen == Screen.Add,
                                onClick = { currentScreen = Screen.Add },
                                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                                label = { Text("Add") }
                            )
                            // CAMBIAR: Reemplazar Sync por Profile
                            NavigationBarItem(
                                selected = screen == Screen.Profile,
                                onClick = { currentScreen = Screen.Profile },
                                icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                        }
                    }
                }
            ) { paddings ->
                Box(Modifier.padding(paddings)) {
                    when (screen) {
                        Screen.Library -> LibraryScreen(
                            vm = vm,
                            onBookClick = { book -> currentScreen = Screen.BookDetail(book.id) }
                        )
                        Screen.Add -> AddBookScreen(vm)
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
                                onNavigateBack = { currentScreen = Screen.Library },
                                onNavigateToSettings = { currentScreen = Screen.Settings }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreen = Screen.Profile },
                                onNavigateToBackup = { currentScreen = Screen.Sync }
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
                Icons.Filled.CloudSync,
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
                    Icons.Filled.CloudSync,
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
                    Icons.Filled.CloudSync,
                    contentDescription = "Sync Error - Tap to retry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

