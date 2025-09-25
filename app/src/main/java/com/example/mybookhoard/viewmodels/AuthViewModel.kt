package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.auth.AuthResult
import com.example.mybookhoard.api.auth.AuthState
import com.example.mybookhoard.repositories.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    // Use the repository's authState directly
    val state: StateFlow<AuthState> = repo.authState

    init {
        // Initialize authentication state when ViewModel is created
        viewModelScope.launch {
            repo.initializeAuthState()
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            repo.login(username, password)
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            repo.register(username, email, password)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }
}