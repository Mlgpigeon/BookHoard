package com.example.mybookhoard.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.*
import com.example.mybookhoard.repository.BookRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthVm(private val repository: BookRepository) : ViewModel() {

    companion object {
        private const val TAG = "AuthVm"
    }

    val authState: StateFlow<AuthState> = repository.authState
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            Log.w(TAG, "Login attempt with empty credentials")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting login for: $identifier")
                val result = repository.login(identifier, password)

                when (result) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Login successful for: ${result.user.username}")
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "Login failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            Log.w(TAG, "Registration attempt with empty fields")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting registration for: $username")
                val result = repository.register(username, email, password)

                when (result) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Registration successful for: ${result.user.username}")
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "Registration failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting logout")
                repository.logout()
                Log.d(TAG, "Logout successful")
            } catch (e: Exception) {
                Log.e(TAG, "Logout error: ${e.message}", e)
            }
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            try {
                repository.getProfile()
            } catch (e: Exception) {
                Log.e(TAG, "Get profile error: ${e.message}", e)
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing connection...")
                if (isAuthenticated()) {
                    repository.getProfile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test error: ${e.message}", e)
            }
        }
    }

    fun clearAuthError() {
        repository.clearAuthError()
    }

    fun clearConnectionError() {
        repository.clearConnectionError()
    }

    fun retryNetworkOperation() {
        viewModelScope.launch {
            Log.d(TAG, "Retrying network operation...")

            if (isAuthenticated()) {
                // Try to sync if authenticated
                repository.syncFromServer()
            } else {
                testConnection()
            }
        }
    }

    // Auth status helpers
    fun isAuthenticated(): Boolean = authState.value is AuthState.Authenticated
    fun isAuthenticating(): Boolean = authState.value is AuthState.Authenticating
    fun hasAuthError(): Boolean = authState.value is AuthState.Error

    fun getCurrentUser(): User? {
        return when (val state = authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    fun getAuthErrorMessage(): String? {
        return when (val state = authState.value) {
            is AuthState.Error -> state.message
            else -> null
        }
    }

    fun getConnectionErrorMessage(): String? {
        return when (val state = connectionState.value) {
            is ConnectionState.Error -> state.message
            else -> null
        }
    }

    // Connection status helpers
    fun isOnline(): Boolean = connectionState.value is ConnectionState.Online
    fun isOffline(): Boolean = connectionState.value is ConnectionState.Offline
    fun isSyncing(): Boolean = connectionState.value is ConnectionState.Syncing
    fun hasConnectionError(): Boolean = connectionState.value is ConnectionState.Error
}