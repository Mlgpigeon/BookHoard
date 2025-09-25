package com.example.mybookhoard.api.auth

import com.example.mybookhoard.data.auth.User

sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: User, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class AuthUser(
    val id: Int?,
    val username: String,
    val email: String?,
    val token: String
)
