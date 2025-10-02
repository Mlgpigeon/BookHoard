// Update AddBookViewModel.kt
// Location: app/src/main/java/com/example/mybookhoard/viewmodels/AddBookViewModel.kt
// Add these properties and methods to the existing class

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
import com.example.mybookhoard.api.books.SagaSuggestion
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

    // Saga suggestions (NEW)
    private val _sagaSuggestions = MutableStateFlow<List<SagaSuggestion>>(emptyList())
    val sagaSuggestions: StateFlow<List<SagaSuggestion>> = _sagaSuggestions.asStateFlow()

    // Selected saga ID and number (NEW)
    private val _selectedSagaId = MutableStateFlow<Long?>(null)
    val selectedSagaId: StateFlow<Long?> = _selectedSagaId.asStateFlow()

    private val _sagaNumber = MutableStateFlow("")
    val sagaNumber: StateFlow<String> = _sagaNumber.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Initial)
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    // Debounce jobs
    private var authorSuggestionsJob: Job? = null
    private var sagaSuggestionsJob: Job? = null

    sealed class AddBookUiState {
        object Initial : AddBookUiState()
        object Loading : AddBookUiState()
        data class Success(val bookTitle: String) : AddBookUiState()
        data class Error(val message: String) : AddBookUiState()
    }

    // Existing methods...
    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title).validate()
    }

    fun updateAuthor(author: String) {
        _formState.value = _formState.value.copy(author = author).validate()

        authorSuggestionsJob?.cancel()
        if (author.length >= 2) {
            authorSuggestionsJob = viewModelScope.launch {
                delay(300)
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

    // NEW: Saga methods
    fun updateSaga(sagaName: String) {
        _formState.value = _formState.value.copy(saga = sagaName).validate()

        // Clear selected saga if user is typing a new name
        if (_selectedSagaId.value != null) {
            _selectedSagaId.value = null
        }

        sagaSuggestionsJob?.cancel()
        if (sagaName.length >= 2) {
            sagaSuggestionsJob = viewModelScope.launch {
                delay(300)
                if (sagaName == _formState.value.saga) {
                    loadSagaSuggestions(sagaName)
                }
            }
        } else {
            _sagaSuggestions.value = emptyList()
        }
    }

    fun selectSagaSuggestion(suggestion: SagaSuggestion) {
        _formState.value = _formState.value.copy(saga = suggestion.name).validate()
        _selectedSagaId.value = suggestion.id
        _sagaSuggestions.value = emptyList()

        // Auto-suggest next number in saga
        val nextNumber = suggestion.totalBooks + 1
        _sagaNumber.value = nextNumber.toString()

        Log.d(TAG, "Selected saga: ${suggestion.name} (ID: ${suggestion.id}), suggested number: $nextNumber")
    }

    fun updateSagaNumber(number: String) {
        // Only allow digits
        val filtered = number.filter { it.isDigit() }
        if (filtered.length <= 4) { // Max 4 digits for saga number
            _sagaNumber.value = filtered
        }
    }

    // Existing methods...
    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description).validate()
    }

    fun updatePublicationYear(year: String) {
        val filteredYear = year.filter { it.isDigit() }
        if (filteredYear.length <= 4) {
            _formState.value = _formState.value.copy(publicationYear = filteredYear).validate()
        }
    }

    fun updateLanguage(language: String) {
        _formState.value = _formState.value.copy(language = language)
    }

    fun updateIsbn(isbn: String) {
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

    private suspend fun loadSagaSuggestions(query: String) {
        try {
            val suggestions = booksCreationApiService.searchSagas(query)
            _sagaSuggestions.value = suggestions
            Log.d(TAG, "Loaded ${suggestions.size} saga suggestions for '$query'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saga suggestions", e)
            _sagaSuggestions.value = emptyList()
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
                // Parse saga number
                val parsedSagaNumber = _sagaNumber.value.toIntOrNull()

                // Determine saga handling
                val sagaId = _selectedSagaId.value
                val sagaName = currentForm.saga.takeIf { it.isNotBlank() }

                Log.d(TAG, "Creating book with saga: name=$sagaName, id=$sagaId, number=$parsedSagaNumber")

                val result = booksCreationApiService.createBook(
                    title = currentForm.title,
                    authorName = currentForm.author.takeIf { it.isNotBlank() },
                    description = currentForm.description.takeIf { it.isNotBlank() },
                    publicationYear = currentForm.publicationYear.toIntOrNull(),
                    language = currentForm.language,
                    isbn = currentForm.isbn.takeIf { it.isNotBlank() },
                    sagaId = sagaId,
                    sagaName = sagaName,
                    sagaNumber = parsedSagaNumber
                )

                when (result) {
                    is BookCreationResult.Success -> {
                        _uiState.value = AddBookUiState.Success(result.book.title)
                        Log.d(TAG, "Book created successfully: ${result.book.title}")
                        resetForm()
                    }
                    is BookCreationResult.Error -> {
                        _uiState.value = AddBookUiState.Error(result.message)
                        Log.e(TAG, "Failed to create book: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddBookUiState.Error(e.message ?: "Unknown error")
                Log.e(TAG, "Exception creating book", e)
            }
        }
    }

    fun resetForm() {
        _formState.value = BookFormState()
        _authorSuggestions.value = emptyList()
        _sagaSuggestions.value = emptyList()
        _selectedSagaId.value = null
        _sagaNumber.value = ""
        _uiState.value = AddBookUiState.Initial
    }

    fun resetUiState() {
        _uiState.value = AddBookUiState.Initial
    }
}