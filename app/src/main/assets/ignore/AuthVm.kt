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

    // FIXED: This method now just uses the current user from auth state
    fun getProfile() {
        // No need to make API call - user info is already in authState
        val currentUser = getCurrentUser()
        if (currentUser != null) {
            Log.d(TAG, "Profile available: ${currentUser.username}")
        } else {
            Log.w(TAG, "No user profile available")
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing connection...")
                if (isAuthenticated()) {
                    val success = repository.testConnection()
                    Log.d(TAG, "Connection test result: $success")
                } else {
                    Log.w(TAG, "Cannot test connection - not authenticated")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test error: ${e.message}", e)
            }
        }
    }

    // FIXED: Removed clearAuthError - errors clear automatically when state changes
    fun clearAuthError() {
        // Auth errors clear automatically when new authentication attempts are made
        Log.d(TAG, "Auth error will clear on next authentication attempt")
    }

    // FIXED: Removed clearConnectionError - errors clear automatically when state changes
    fun clearConnectionError() {
        // Connection errors clear automatically when connection state changes
        Log.d(TAG, "Connection error will clear on next connection attempt")
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
        return repository.getCurrentUser()
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