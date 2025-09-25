package com.example.mybookhoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
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
import com.example.mybookhoard.repositories.AuthRepository
import com.example.mybookhoard.data.auth.UserPreferences
import com.example.mybookhoard.screens.AuthScreen
import com.example.mybookhoard.screens.HomeScreen
import com.example.mybookhoard.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = UserPreferences(this)
        val api = AuthApi(this)
        val repo = AuthRepository(api,prefs)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repo) as T
            }
        }

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val vm: AuthViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsState()

                NavHost(navController = nav, startDestination = "auth") {
                    composable("auth") {
                        AuthScreen(
                            authState = state,
                            onLogin = { id, pass -> vm.login(id, pass) },
                            onRegister = { u, e, p -> vm.register(u, e, p) }
                        )
                    }
                    composable("home") {
                        val user = (state as? AuthState.Authenticated)?.user
                        if (user != null) HomeScreen(user, onLogout = { vm.logout() })
                    }
                }

                LaunchedEffect(state) {
                    when (state) {
                        is AuthState.Authenticated -> nav.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
