package com.example.mybookhoard.repositories

import android.content.Context
import android.util.Log
import com.example.mybookhoard.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Manages authentication state and session verification
 */
class AuthStateManager(
    private val context: Context,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "AuthStateManager"
        private const val SESSION_VERIFICATION_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val managerScope = CoroutineScope(Dispatchers.IO)

    init {
        initializeAuthState()
    }

    private fun initializeAuthState() {
        managerScope.launch {
            Log.d(TAG, "Initializing auth state...")

            if (!apiService.hasValidSession()) {
                Log.d(TAG, "No valid session found")
                _authState.value = AuthState.NotAuthenticated
                return@launch
            }

            val savedUser = apiService.getSavedUser()
            val token = apiService.getAuthToken()

            if (savedUser == null || token == null) {
                Log.w(TAG, "Incomplete session data, clearing")
                apiService.clearUserSession()
                _authState.value = AuthState.NotAuthenticated
                return@launch
            }

            Log.d(TAG, "Found saved session for user: ${savedUser.username}")

            // Set authenticated state immediately with saved data
            _authState.value = AuthState.Authenticated(savedUser, token)

            // Verify session in background
            verifySession()
        }
    }

    private fun verifySession() {
        managerScope.launch {
            try {
                Log.d(TAG, "Verifying session...")
                when (val result = apiService.getProfile()) {
                    is ApiResult.Success -> {
                        val currentState = _authState.value
                        if (currentState is AuthState.Authenticated) {
                            _authState.value = AuthState.Authenticated(result.data, currentState.token)
                        }
                        Log.d(TAG, "Session verified successfully")

                        // Schedule periodic verification
                        schedulePeriodicVerification()
                    }
                    is ApiResult.Error -> {
                        Log.w(TAG, "Session verification failed: ${result.message}")
                        if (result.message.contains("401") || result.message.contains("unauthorized")) {
                            logout()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Session verification error: ${e.message}")
            }
        }
    }

    private suspend fun schedulePeriodicVerification() {
        delay(SESSION_VERIFICATION_INTERVAL)
        if (_authState.value is AuthState.Authenticated) {
            verifySession()
        }
    }

    suspend fun login(username: String, password: String): AuthResult {
        _authState.value = AuthState.Authenticating

        return when (val result = apiService.login(username, password)) {
            is AuthResult.Success -> {
                _authState.value = AuthState.Authenticated(result.user, result.token)
                Log.d(TAG, "Login successful for user: ${result.user.username}")
                result
            }
            is AuthResult.Error -> {
                _authState.value = AuthState.Error(result.message)
                Log.e(TAG, "Login failed: ${result.message}")
                result
            }
        }
    }

    suspend fun register(username: String, email: String, password: String): AuthResult {
        _authState.value = AuthState.Authenticating

        return when (val result = apiService.register(username, email, password)) {
            is AuthResult.Success -> {
                _authState.value = AuthState.Authenticated(result.user, result.token)
                Log.d(TAG, "Registration successful for user: ${result.user.username}")
                result
            }
            is AuthResult.Error -> {
                _authState.value = AuthState.Error(result.message)
                Log.e(TAG, "Registration failed: ${result.message}")
                result
            }
        }
    }

    fun logout() {
        managerScope.launch {
            try {
                apiService.clearUserSession()
                _authState.value = AuthState.NotAuthenticated
                Log.d(TAG, "User logged out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
                _authState.value = AuthState.NotAuthenticated
            }
        }
    }

    fun isAuthenticated(): Boolean {
        return _authState.value is AuthState.Authenticated
    }

    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    fun getCurrentToken(): String? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.token
            else -> null
        }
    }
}