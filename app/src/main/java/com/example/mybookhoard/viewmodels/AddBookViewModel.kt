package com.example.mybookhoard.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.mybookhoard.api.books.BooksCreationApiService
import com.example.mybookhoard.api.books.BookCreationResult
import com.example.mybookhoard.components.form.BookFormState
import com.example.mybookhoard.components.form.isValid
import com.example.mybookhoard.components.form.validate

class AddBookViewModel(
    private val booksCreationApiService: BooksCreationApiService
) : ViewModel() {

    companion object {
        private const val TAG = "AddBookViewModel"
    }

    // Form state
    private val _formState = MutableStateFlow(BookFormState())
    val formState: StateFlow<BookFormState> = _formState.asStateFlow()

    // Author suggestions
    private val _authorSuggestions = MutableStateFlow<List<String>>(emptyList())
    val authorSuggestions: StateFlow<List<String>> = _authorSuggestions.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Initial)
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    // Debounce job for author suggestions
    private var authorSuggestionsJob: Job? = null

    sealed class AddBookUiState {
        object Initial : AddBookUiState()
        object Loading : AddBookUiState()
        data class Success(val bookTitle: String) : AddBookUiState()
        data class Error(val message: String) : AddBookUiState()
    }

    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title).validate()
    }

    fun updateAuthor(author: String) {
        _formState.value = _formState.value.copy(author = author).validate()

        // Get author suggestions with debounce
        authorSuggestionsJob?.cancel()
        if (author.length >= 2) {
            authorSuggestionsJob = viewModelScope.launch {
                delay(300) // Debounce
                if (author == _formState.value.author) {
                    loadAuthorSuggestions(author)
                }
            }
        } else {
            _authorSuggestions.value = emptyList()
        }
    }

    fun selectAuthorSuggestion(author: String) {
        _formState.value = _formState.value.copy(author = author).validate()
        _authorSuggestions.value = emptyList()
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description).validate()
    }

    fun updatePublicationYear(year: String) {
        // Only allow digits
        val filteredYear = year.filter { it.isDigit() }
        if (filteredYear.length <= 4) {
            _formState.value = _formState.value.copy(publicationYear = filteredYear).validate()
        }
    }

    fun updateLanguage(language: String) {
        _formState.value = _formState.value.copy(language = language)
    }

    fun updateIsbn(isbn: String) {
        // Allow digits, spaces, and hyphens, and 'X' for ISBN-10
        val filteredIsbn = isbn.filter { it.isDigit() || it == '-' || it == ' ' || it.uppercaseChar() == 'X' }
        _formState.value = _formState.value.copy(isbn = filteredIsbn).validate()
    }

    private suspend fun loadAuthorSuggestions(query: String) {
        try {
            val suggestions = booksCreationApiService.searchAuthors(query)
            _authorSuggestions.value = suggestions
            Log.d(TAG, "Loaded ${suggestions.size} author suggestions for '$query'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load author suggestions", e)
            _authorSuggestions.value = emptyList()
        }
    }

    fun createBook() {
        val currentForm = _formState.value.validate()
        _formState.value = currentForm

        if (!currentForm.isValid()) {
            Log.w(TAG, "Form validation failed")
            return
        }

        _uiState.value = AddBookUiState.Loading

        viewModelScope.launch {
            try {
                val result = booksCreationApiService.createBook(
                    title = currentForm.title,
                    authorName = currentForm.author.takeIf { it.isNotBlank() },
                    description = currentForm.description.takeIf { it.isNotBlank() },
                    publicationYear = currentForm.publicationYear.takeIf { it.isNotBlank() }?.toIntOrNull(),
                    language = currentForm.language,
                    isbn = currentForm.isbn.takeIf { it.isNotBlank() }
                )

                when (result) {
                    is BookCreationResult.Success -> {
                        Log.d(TAG, "Book created successfully: ${result.book.title}")
                        _uiState.value = AddBookUiState.Success(result.book.title)

                        // Reset form after successful creation
                        resetForm()
                    }
                    is BookCreationResult.Error -> {
                        Log.e(TAG, "Failed to create book: ${result.message}")
                        _uiState.value = AddBookUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating book", e)
                _uiState.value = AddBookUiState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    private fun resetForm() {
        _formState.value = BookFormState()
        _authorSuggestions.value = emptyList()
        // Reset UI state to initial after a delay
        viewModelScope.launch {
            delay(1000) // Give time for success message to show
            _uiState.value = AddBookUiState.Initial
        }
    }

    fun resetUiState() {
        _uiState.value = AddBookUiState.Initial
    }
}