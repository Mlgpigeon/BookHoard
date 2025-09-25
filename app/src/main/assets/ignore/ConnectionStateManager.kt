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
 * Manages connection state and connectivity monitoring
 */
class ConnectionStateManager(
    private val context: Context,
    private val apiService: ApiService,
    private val authStateManager: AuthStateManager
) {
    companion object {
        private const val TAG = "ConnectionStateManager"
        private const val CONNECTION_CHECK_INTERVAL = 30 * 1000L // 30 seconds
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Offline)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val managerScope = CoroutineScope(Dispatchers.IO)

    init {
        // Monitor auth state changes
        managerScope.launch {
            authStateManager.authState.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        Log.d(TAG, "User authenticated, testing connection...")
                        testConnection()
                    }
                    is AuthState.NotAuthenticated -> {
                        Log.d(TAG, "User not authenticated, setting offline")
                        _connectionState.value = ConnectionState.Offline
                    }
                    is AuthState.Error -> {
                        Log.d(TAG, "Auth error, setting connection error")
                        _connectionState.value = ConnectionState.Error(authState.message)
                    }
                    is AuthState.Authenticating -> {
                        // Keep current state during authentication
                    }
                }
            }
        }
    }

    suspend fun testConnection(): Boolean {
        if (!authStateManager.isAuthenticated()) {
            _connectionState.value = ConnectionState.Offline
            return false
        }

        return try {
            Log.d(TAG, "Testing connection to server...")
            when (val result = apiService.testConnection()) {
                is ApiResult.Success -> {
                    _connectionState.value = ConnectionState.Online
                    Log.d(TAG, "Connection test successful")
                    schedulePeriodicCheck()
                    true
                }
                is ApiResult.Error -> {
                    _connectionState.value = ConnectionState.Error(result.message)
                    Log.w(TAG, "Connection test failed: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Connection test error: ${e.message}"
            _connectionState.value = ConnectionState.Error(errorMsg)
            Log.e(TAG, errorMsg)
            false
        }
    }

    private suspend fun schedulePeriodicCheck() {
        delay(CONNECTION_CHECK_INTERVAL)
        if (authStateManager.isAuthenticated()) {
            testConnection()
        }
    }

    fun setSyncing() {
        _connectionState.value = ConnectionState.Syncing
        Log.d(TAG, "Connection state set to syncing")
    }

    fun setOnline() {
        _connectionState.value = ConnectionState.Online
        Log.d(TAG, "Connection state set to online")
    }

    fun setError(message: String) {
        _connectionState.value = ConnectionState.Error(message)
        Log.w(TAG, "Connection state set to error: $message")
    }

    fun setOffline() {
        _connectionState.value = ConnectionState.Offline
        Log.d(TAG, "Connection state set to offline")
    }

    fun isOnline(): Boolean {
        return _connectionState.value == ConnectionState.Online
    }

    fun isSyncing(): Boolean {
        return _connectionState.value == ConnectionState.Syncing
    }

    fun isOffline(): Boolean {
        return _connectionState.value == ConnectionState.Offline
    }

    fun getCurrentState(): ConnectionState {
        return _connectionState.value
    }
}