package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mybookhoard.repositories.AuthRepository
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.repositories.UserBookRepository

class ViewModelFactory(
    private val authRepository: AuthRepository,
    private val bookRepository: BookRepository,
    private val userBookRepository: UserBookRepository,
    private val currentUserId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            AuthViewModel::class.java -> {
                AuthViewModel(authRepository) as T
            }
            SearchViewModel::class.java -> {
                SearchViewModel(
                    bookRepository = bookRepository,
                    userBookRepository = userBookRepository,
                    currentUserId = currentUserId
                ) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}