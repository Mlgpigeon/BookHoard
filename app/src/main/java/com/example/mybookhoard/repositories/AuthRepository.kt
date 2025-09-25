package com.example.mybookhoard.repositories

import android.util.Log
import com.example.mybookhoard.api.AuthApi
import com.example.mybookhoard.api.AuthResult
import com.example.mybookhoard.api.AuthState
import com.example.mybookhoard.api.AuthUser
import com.example.mybookhoard.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class AuthRepository(private val api: AuthApi,
                     private val prefs: UserPreferences) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)

    private val TAG = "AuthRepository";

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
        val token = prefs.token
        prefs.clear()
        // opcional: AuthApi.logout(token)
    }
}
