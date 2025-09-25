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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mybookhoard.api.auth.AuthApi
import com.example.mybookhoard.api.auth.AuthState
import com.example.mybookhoard.components.navigation.BottomNavigationBar
import com.example.mybookhoard.repositories.AuthRepository
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.repositories.UserBookRepository
import com.example.mybookhoard.data.auth.UserPreferences
import com.example.mybookhoard.screens.AuthScreen
import com.example.mybookhoard.screens.ProfileScreen
import com.example.mybookhoard.screens.SearchScreen
import com.example.mybookhoard.viewmodels.AuthViewModel
import com.example.mybookhoard.viewmodels.SearchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = UserPreferences(this)
        val api = AuthApi(this)
        val repo = AuthRepository(api, prefs)

        // Initialize repositories
        val bookRepository = BookRepository.getInstance(this)
        val userBookRepository = UserBookRepository.getInstance(this)

        val authFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repo) as T
            }
        }

        val searchFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(
                    bookRepository = bookRepository,
                    userBookRepository = userBookRepository,
                    currentUserId = 1L // TODO: Get from auth
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
                                        onProfileClick = { nav.navigate("profile") }
                                    )
                                }
                            ) { paddingValues ->
                                Box(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                                    SearchScreen(searchViewModel = searchVm)
                                }
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
                                        onProfileClick = { /* Already on profile */ }
                                    )
                                }
                            ) { paddingValues ->
                                Box(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                                    ProfileScreen(user, onLogout = { authVm.logout() })
                                }
                            }
                        }
                    }
                }

                // Handle navigation based on auth state changes
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Authenticated -> {
                            nav.navigate("search") { // Changed default to search
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                        is AuthState.NotAuthenticated -> {
                            nav.navigate("auth") {
                                popUpTo("search") { inclusive = true }
                                popUpTo("profile") { inclusive = true }
                            }
                        }
                        is AuthState.Error -> {
                            nav.navigate("auth") {
                                popUpTo("search") { inclusive = true }
                                popUpTo("profile") { inclusive = true }
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
}