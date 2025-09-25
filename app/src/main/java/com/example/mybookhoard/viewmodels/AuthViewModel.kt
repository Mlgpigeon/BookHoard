package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.auth.AuthResult
import com.example.mybookhoard.api.auth.AuthState
import com.example.mybookhoard.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val state: StateFlow<AuthState> = _state

    fun login(username: String, password: String) {
        viewModelScope.launch {
            when (val result = repo.login(username, password)) {
                is AuthResult.Success ->
                    _state.value = AuthState.Authenticated(result.user, result.token)
                is AuthResult.Error ->
                    _state.value = AuthState.Error(result.message)
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            when (val result = repo.register(username, email, password)) {
                is AuthResult.Success ->
                    _state.value = AuthState.Authenticated(result.user, result.token)
                is AuthResult.Error ->
                    _state.value = AuthState.Error(result.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _state.value = AuthState.NotAuthenticated
        }
    }
}
