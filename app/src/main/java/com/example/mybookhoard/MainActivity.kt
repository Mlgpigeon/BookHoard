package com.example.mybookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mybookhoard.api.auth.AuthApi
import com.example.mybookhoard.api.auth.AuthState
import com.example.mybookhoard.api.books.BooksApiService
import com.example.mybookhoard.api.books.UserBooksApiService
import com.example.mybookhoard.api.books.BooksCreationApiService
import com.example.mybookhoard.api.books.SagasApiService
import com.example.mybookhoard.components.navigation.BottomNavigationBar
import com.example.mybookhoard.components.sagas.BookPickerDialog
import com.example.mybookhoard.repositories.AuthRepository
import com.example.mybookhoard.repositories.UserBookRepository
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.data.auth.UserPreferences
import com.example.mybookhoard.screens.AuthScreen
import com.example.mybookhoard.screens.ProfileScreen
import com.example.mybookhoard.screens.SearchScreen
import com.example.mybookhoard.screens.LibraryScreen
import com.example.mybookhoard.screens.AddBookScreen
import com.example.mybookhoard.screens.SagaEditorScreen
import com.example.mybookhoard.screens.SagasManagementScreen
import com.example.mybookhoard.viewmodels.AuthViewModel
import com.example.mybookhoard.viewmodels.SearchViewModel
import com.example.mybookhoard.viewmodels.LibraryViewModel
import com.example.mybookhoard.viewmodels.AddBookViewModel
import com.example.mybookhoard.viewmodels.SagasViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = UserPreferences(this)
        val authApi = AuthApi(this)
        val authRepository = AuthRepository(authApi, prefs)

        // Initialize modularized API services
        val booksApiService = BooksApiService(this)
        val userBooksApiService = UserBooksApiService(this)
        val booksCreationApiService = BooksCreationApiService(this)
        val sagasApiService = SagasApiService(this)

        // Initialize repositories
        val userBookRepository = UserBookRepository.getInstance(this)
        val bookRepository = BookRepository.getInstance(this)

        val authFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(authRepository) as T
            }
        }

        val searchFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(
                    booksApiService = booksApiService,
                    userBooksApiService = userBooksApiService,
                    currentUserId = getCurrentUserId()
                ) as T
            }
        }

        val libraryFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(
                    userBookRepository = userBookRepository,
                    bookRepository = bookRepository,
                    userId = getCurrentUserId(),
                    userBooksApiService = userBooksApiService
                ) as T
            }
        }

        val addBookFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AddBookViewModel(
                    booksCreationApiService = booksCreationApiService,
                    userBooksApiService = userBooksApiService  // NUEVA DEPENDENCIA
                ) as T
            }
        }

        val sagasFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SagasViewModel(
                    sagasApiService = sagasApiService,
                    booksApiService = booksApiService
                ) as T
            }
        }

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val authVm: AuthViewModel = viewModel(factory = authFactory)
                val authState by authVm.state.collectAsState()

                NavHost(navController = nav, startDestination = "auth") {
                    composable("auth") {
                        AuthScreen(
                            authState = authState,
                            onLogin = { id, pass -> authVm.login(id, pass) },
                            onRegister = { u, e, p -> authVm.register(u, e, p) }
                        )
                    }

                    composable("search") {
                        val searchVm: SearchViewModel = viewModel(factory = searchFactory)
                        val user = (authState as? AuthState.Authenticated)?.user
                        if (user != null) {
                            Scaffold(
                                bottomBar = {
                                    BottomNavigationBar(
                                        isSearchSelected = true,
                                        onSearchClick = { /* Already on search */ },
                                        onLibraryClick = { nav.navigate("library") },
                                        onProfileClick = { nav.navigate("profile") }
                                    )
                                }
                            ) { paddingValues ->
                                Box(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                                    SearchScreen(
                                        searchViewModel = searchVm,
                                        onAddBookClick = { nav.navigate("add_book") }
                                    )
                                }
                            }
                        }
                    }

                    composable("add_book") {
                        val addBookVm: AddBookViewModel = viewModel(factory = addBookFactory)
                        val user = (authState as? AuthState.Authenticated)?.user
                        if (user != null) {
                            AddBookScreen(
                                onNavigateBack = { nav.navigateUp() },
                                onBookCreated = {
                                    nav.navigateUp()
                                    // Optionally navigate to library to see the new book
                                    nav.navigate("library")
                                },
                                addBookViewModel = addBookVm
                            )
                        }
                    }

                    composable("library") {
                        val libraryVm: LibraryViewModel = viewModel(factory = libraryFactory)
                        val user = (authState as? AuthState.Authenticated)?.user
                        if (user != null) {
                            Scaffold(
                                bottomBar = {
                                    BottomNavigationBar(
                                        isLibrarySelected = true,
                                        onSearchClick = { nav.navigate("search") },
                                        onLibraryClick = { /* Already on library */ },
                                        onProfileClick = { nav.navigate("profile") }
                                    )
                                }
                            ) { paddingValues ->
                                Box(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                                    LibraryScreen(
                                        libraryViewModel = libraryVm
                                    )
                                }
                            }
                        }
                    }

                    composable("sagas") {
                        val sagasVm: SagasViewModel = viewModel(factory = sagasFactory)
                        val user = (authState as? AuthState.Authenticated)?.user
                        if (user != null) {
                            SagasManagementScreen(
                                onNavigateBack = { nav.navigateUp() },
                                onNavigateToEditor = { sagaId ->
                                    if (sagaId != null) {
                                        nav.navigate("sagas/edit/$sagaId")
                                    } else {
                                        nav.navigate("sagas/create")
                                    }
                                },
                                sagasViewModel = sagasVm
                            )
                        }
                    }

                    composable("sagas/create") {
                        val sagasVm: SagasViewModel = viewModel(factory = sagasFactory)
                        val booksVm: SearchViewModel = viewModel(factory = searchFactory)
                        var showBookPicker by remember { mutableStateOf(false) }

                        SagaEditorScreen(
                            sagaId = null,
                            onNavigateBack = { nav.navigateUp() },
                            onNavigateToBookPicker = { showBookPicker = true },
                            onSagaSaved = { nav.navigateUp() },
                            sagasViewModel = sagasVm
                        )

                        // Book picker dialog
                        if (showBookPicker) {
                            val searchUiState by booksVm.uiState.collectAsState()
                            val books = when (val state = searchUiState) {
                                is SearchViewModel.SearchUiState.Success -> state.books.map { it.book }
                                else -> emptyList()
                            }

                            BookPickerDialog(
                                onDismiss = { showBookPicker = false },
                                onBookSelected = { book ->
                                    sagasVm.addBookToSaga(book)
                                    showBookPicker = false
                                },
                                availableBooks = books,
                                isLoading = searchUiState is SearchViewModel.SearchUiState.Loading,
                                onSearch = { query ->
                                    if (query.isNotBlank()) {
                                        booksVm.updateSearchQuery(query)
                                        booksVm.performSearch()
                                    }
                                }
                            )
                        }
                    }

                    composable("sagas/edit/{sagaId}") { backStackEntry ->
                        val sagaId = backStackEntry.arguments?.getString("sagaId")?.toLongOrNull()
                        val sagasVm: SagasViewModel = viewModel(factory = sagasFactory)
                        val booksVm: SearchViewModel = viewModel(factory = searchFactory)
                        var showBookPicker by remember { mutableStateOf(false) }

                        if (sagaId != null) {
                            SagaEditorScreen(
                                sagaId = sagaId,
                                onNavigateBack = { nav.navigateUp() },
                                onNavigateToBookPicker = { showBookPicker = true },
                                onSagaSaved = { nav.navigateUp() },
                                sagasViewModel = sagasVm
                            )

                            // Book picker dialog
                            if (showBookPicker) {
                                val searchUiState by booksVm.uiState.collectAsState()
                                val books = when (val state = searchUiState) {
                                    is SearchViewModel.SearchUiState.Success -> state.books.map { it.book }
                                    else -> emptyList()
                                }

                                BookPickerDialog(
                                    onDismiss = { showBookPicker = false },
                                    onBookSelected = { book ->
                                        sagasVm.addBookToSaga(book)
                                        showBookPicker = false
                                    },
                                    availableBooks = books,
                                    isLoading = searchUiState is SearchViewModel.SearchUiState.Loading,
                                    onSearch = { query ->
                                        if (query.isNotBlank()) {
                                            booksVm.updateSearchQuery(query)
                                            booksVm.performSearch()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    composable("profile") {
                        val user = (authState as? AuthState.Authenticated)?.user
                        if (user != null) {
                            Scaffold(
                                bottomBar = {
                                    BottomNavigationBar(
                                        isProfileSelected = true,
                                        onSearchClick = { nav.navigate("search") },
                                        onLibraryClick = { nav.navigate("library") },
                                        onProfileClick = { /* Already on profile */ }
                                    )
                                }
                            ) { paddingValues ->
                                Box(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                                    ProfileScreen(
                                        user = user,
                                        onLogout = { authVm.logout() },
                                        onNavigateToSagas = { nav.navigate("sagas") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Handle navigation based on auth state changes
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Authenticated -> {
                            nav.navigate("library") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                        is AuthState.NotAuthenticated -> {
                            nav.navigate("auth") {
                                popUpTo("search") { inclusive = true }
                                popUpTo("library") { inclusive = true }
                                popUpTo("profile") { inclusive = true }
                                popUpTo("add_book") { inclusive = true }
                                popUpTo("sagas") { inclusive = true }
                            }
                        }
                        is AuthState.Error -> {
                            nav.navigate("auth") {
                                popUpTo("search") { inclusive = true }
                                popUpTo("library") { inclusive = true }
                                popUpTo("profile") { inclusive = true }
                                popUpTo("add_book") { inclusive = true }
                                popUpTo("sagas") { inclusive = true }
                            }
                        }
                        is AuthState.Authenticating -> {
                            // No navigation needed during authentication
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("bookhoard_auth", MODE_PRIVATE)
        return prefs.getLong("user_id", -1L)
    }
}