package com.example.mybookhoard.repositories

import android.util.Log
import com.example.mybookhoard.api.auth.AuthApi
import com.example.mybookhoard.api.auth.AuthResult
import com.example.mybookhoard.api.auth.AuthState
import com.example.mybookhoard.data.auth.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class AuthRepository(private val api: AuthApi,
                     private val prefs: UserPreferences) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val TAG = "AuthRepository"

    // NEW: Initialize authentication state on repository creation
    suspend fun initializeAuthState() {
        Log.d(TAG, "Initializing authentication state...")

        // Check if there's a saved session
        val savedSession = api.getSavedSession()
        if (savedSession is AuthResult.Success) {
            Log.d(TAG, "Found saved session for user: ${savedSession.user.username}")
            _authState.value = AuthState.Authenticated(savedSession.user, savedSession.token)
        } else {
            Log.d(TAG, "No saved session found")
            _authState.value = AuthState.NotAuthenticated
        }
    }

    suspend fun login(username: String, password: String): AuthResult {
        _authState.value = AuthState.Authenticating

        return when (val result = api.login(username, password)) {
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

        return when (val result = api.register(username, email, password)) {
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

    suspend fun logout() {
        Log.d(TAG, "Logging out user")
        api.clearUserSession()
        prefs.clear()
        _authState.value = AuthState.NotAuthenticated
    }
}