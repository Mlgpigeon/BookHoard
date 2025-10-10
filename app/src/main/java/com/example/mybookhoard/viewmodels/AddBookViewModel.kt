package com.example.mybookhoard.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.ImageUploadService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.mybookhoard.api.books.BooksCreationApiService
import com.example.mybookhoard.api.books.UserBooksApiService
import com.example.mybookhoard.api.books.BookCreationResult
import com.example.mybookhoard.api.books.BooksActionResult
import com.example.mybookhoard.api.books.SagaSuggestion
import com.example.mybookhoard.components.form.BookFormState
import com.example.mybookhoard.components.form.isValid
import com.example.mybookhoard.components.form.validate
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

class AddBookViewModel(
    private val booksCreationApiService: BooksCreationApiService,
    private val userBooksApiService: UserBooksApiService,
    private val imageUploadService: ImageUploadService
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

    // Saga suggestions
    private val _sagaSuggestions = MutableStateFlow<List<SagaSuggestion>>(emptyList())
    val sagaSuggestions: StateFlow<List<SagaSuggestion>> = _sagaSuggestions.asStateFlow()

    // Selected saga ID and number
    private val _selectedSagaId = MutableStateFlow<Long?>(null)
    val selectedSagaId: StateFlow<Long?> = _selectedSagaId.asStateFlow()

    private val _sagaNumber = MutableStateFlow("")
    val sagaNumber: StateFlow<String> = _sagaNumber.asStateFlow()

    // Wishlist status selection (NEW)
    private val _selectedWishlistStatus = MutableStateFlow<UserBookWishlistStatus?>(null)
    val selectedWishlistStatus: StateFlow<UserBookWishlistStatus?> = _selectedWishlistStatus.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

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
        _sagaNumber.value = (suggestion.totalBooks + 1).toString()
        _sagaSuggestions.value = emptyList()
        Log.d(TAG, "Selected saga: ${suggestion.name} (ID: ${suggestion.id}), auto-filled number: ${suggestion.totalBooks + 1}")
    }

    fun updateSagaNumber(number: String) {
        val filtered = number.filter { it.isDigit() }
        _sagaNumber.value = filtered
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description).validate()
    }

    fun updatePublicationYear(year: String) {
        val filtered = year.filter { it.isDigit() }.take(4)
        _formState.value = _formState.value.copy(publicationYear = filtered).validate()
    }

    fun updateLanguage(language: String) {
        _formState.value = _formState.value.copy(language = language).validate()
    }

    fun updateIsbn(isbn: String) {
        val filteredIsbn = isbn.filter { it.isDigit() || it == '-' || it == ' ' || it.uppercaseChar() == 'X' }
        _formState.value = _formState.value.copy(isbn = filteredIsbn).validate()
    }

    // NEW: Update wishlist status
    fun updateWishlistStatus(status: UserBookWishlistStatus?) {
        _selectedWishlistStatus.value = status
        Log.d(TAG, "Wishlist status updated to: ${status?.name ?: "none"}")
    }

    fun updateImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        Log.d(TAG, "Image URI updated: $uri")
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
                var imageUrl: String? = null
                val imageUri = _selectedImageUri.value
                if (imageUri != null) {
                    Log.d(TAG, "Uploading book cover image...")
                    when (val uploadResult = imageUploadService.uploadBookCover(imageUri)) {
                        is ImageUploadService.ImageUploadResult.Success -> {
                            imageUrl = uploadResult.url
                            Log.d(TAG, "Image uploaded successfully: $imageUrl")
                        }
                        is ImageUploadService.ImageUploadResult.Error -> {
                            Log.w(TAG, "Image upload failed: ${uploadResult.message}")
                            // Continue without image
                        }
                    }
                }

                // Step 1: Create the book
                val result = booksCreationApiService.createBook(
                    title = currentForm.title,
                    authorName = currentForm.author.takeIf { it.isNotBlank() },
                    description = currentForm.description.takeIf { it.isNotBlank() },
                    publicationYear = currentForm.publicationYear.toIntOrNull(),
                    language = currentForm.language,
                    isbn = currentForm.isbn.takeIf { it.isNotBlank() },
                    sagaId = sagaId,
                    sagaName = sagaName,
                    sagaNumber = parsedSagaNumber,
                    coverImageUrl = imageUrl
                )

                when (result) {
                    is BookCreationResult.Success -> {
                        val createdBook = result.book
                        Log.d(TAG, "Book created successfully: ${createdBook.title} (ID: ${createdBook.id})")

                        // Step 2: If wishlist status is selected, create UserBook
                        val wishlistStatus = _selectedWishlistStatus.value
                        if (wishlistStatus != null) {
                            Log.d(TAG, "Creating UserBook with wishlist status: ${wishlistStatus.name}")

                            val bookId = createdBook.id

                            if (bookId == null) {
                                Log.e(TAG, "Book created but ID is null")
                                _uiState.value = AddBookUiState.Error("Book created but failed to get ID")
                                return@launch
                            }

                            when (val userBookResult = userBooksApiService.addBookToCollection(
                                bookId = createdBook.id,
                                wishlistStatus = wishlistStatus.name
                            )) {
                                is BooksActionResult.Success -> {
                                    Log.d(TAG, "UserBook created successfully")
                                    _uiState.value = AddBookUiState.Success(createdBook.title)
                                }
                                is BooksActionResult.Error -> {
                                    Log.w(TAG, "Book created but failed to add to collection: ${userBookResult.message}")
                                    // Still consider it a success since the book was created
                                    _uiState.value = AddBookUiState.Success(createdBook.title)
                                }
                            }
                        } else {
                            Log.d(TAG, "No wishlist status selected, skipping UserBook creation")
                            _uiState.value = AddBookUiState.Success(createdBook.title)
                        }

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
        _selectedWishlistStatus.value = null
        _uiState.value = AddBookUiState.Initial
        _selectedImageUri.value = null
    }

    fun resetUiState() {
        _uiState.value = AddBookUiState.Initial
    }
}