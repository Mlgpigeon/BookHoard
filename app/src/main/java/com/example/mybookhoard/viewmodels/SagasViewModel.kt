package com.example.mybookhoard.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.books.*
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.Saga
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SagasViewModel(
    private val sagasApiService: SagasApiService,
    private val booksApiService: BooksApiService
) : ViewModel() {

    companion object {
        private const val TAG = "SagasViewModel"
    }

    // All sagas list
    private val _sagas = MutableStateFlow<List<Saga>>(emptyList())
    val sagas: StateFlow<List<Saga>> = _sagas.asStateFlow()

    // Currently editing saga
    private val _currentSaga = MutableStateFlow<Saga?>(null)
    val currentSaga: StateFlow<Saga?> = _currentSaga.asStateFlow()

    // Books being added to saga (with temporary order)
    private val _sagaBooks = MutableStateFlow<List<BookWithOrder>>(emptyList())
    val sagaBooks: StateFlow<List<BookWithOrder>> = _sagaBooks.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow<SagaUiState>(SagaUiState.Initial)
    val uiState: StateFlow<SagaUiState> = _uiState.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    sealed class SagaUiState {
        object Initial : SagaUiState()
        object Loading : SagaUiState()
        object SagasList : SagaUiState()
        object Creating : SagaUiState()
        data class Editing(val sagaId: Long) : SagaUiState()
        data class Success(val message: String) : SagaUiState()
        data class Error(val message: String) : SagaUiState()
    }

    data class BookWithOrder(
        val book: Book,
        val order: Int
    )

    init {
        loadSagas()
    }

    fun loadSagas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = sagasApiService.getAllSagas()) {
                is SagasResult.Success -> {
                    _sagas.value = result.sagas
                    Log.d(TAG, "Loaded ${result.sagas.size} sagas")
                }
                is SagasResult.Error -> {
                    _error.value = result.message
                    Log.e(TAG, "Failed to load sagas: ${result.message}")
                }
            }

            _isLoading.value = false
        }
    }

    fun startCreating() {
        _uiState.value = SagaUiState.Creating
        _currentSaga.value = null
        _sagaBooks.value = emptyList()
    }

    fun startEditing(sagaId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = SagaUiState.Editing(sagaId)

            // Load saga details
            when (val sagaResult = sagasApiService.getSaga(sagaId)) {
                is SagaResult.Success -> {
                    _currentSaga.value = sagaResult.saga

                    // Load books in saga
                    when (val booksResult = sagasApiService.getBooksInSaga(sagaId)) {
                        is BooksResult.Success -> {
                            _sagaBooks.value = booksResult.books.mapIndexed { index, book ->
                                BookWithOrder(book, book.sagaNumber ?: (index + 1))
                            }.sortedBy { it.order }
                        }
                        is BooksResult.Error -> {
                            _error.value = booksResult.message
                        }
                    }
                }
                is SagaResult.Error -> {
                    _error.value = sagaResult.message
                    _uiState.value = SagaUiState.SagasList
                }
            }

            _isLoading.value = false
        }
    }

    fun addBookToSaga(book: Book) {
        val currentBooks = _sagaBooks.value

        // Check if book is already in the list
        if (currentBooks.any { it.book.id == book.id }) {
            _error.value = "Book is already in the saga"
            return
        }

        val newOrder = currentBooks.size + 1
        _sagaBooks.value = currentBooks + BookWithOrder(book, newOrder)
    }

    fun removeBookFromSaga(bookId: Long) {
        val currentBooks = _sagaBooks.value
        _sagaBooks.value = currentBooks.filter { it.book.id != bookId }
            .mapIndexed { index, bookWithOrder ->
                bookWithOrder.copy(order = index + 1)
            }
    }

    fun reorderBooks(fromIndex: Int, toIndex: Int) {
        val currentBooks = _sagaBooks.value.toMutableList()

        if (fromIndex < 0 || fromIndex >= currentBooks.size ||
            toIndex < 0 || toIndex >= currentBooks.size) {
            return
        }

        val item = currentBooks.removeAt(fromIndex)
        currentBooks.add(toIndex, item)

        // Update orders
        _sagaBooks.value = currentBooks.mapIndexed { index, bookWithOrder ->
            bookWithOrder.copy(order = index + 1)
        }
    }

    fun createSaga(
        name: String,
        description: String?,
        primaryAuthorId: Long?,
        isCompleted: Boolean
    ) {
        if (name.isBlank()) {
            _error.value = "Saga name is required"
            return
        }

        if (_sagaBooks.value.isEmpty()) {
            _error.value = "Please add at least one book to the saga"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            // Create saga
            val totalBooks = _sagaBooks.value.size
            when (val result = sagasApiService.createSaga(
                name = name,
                description = description,
                primaryAuthorId = primaryAuthorId,
                totalBooks = totalBooks,
                isCompleted = isCompleted
            )) {
                is SagaResult.Success -> {
                    val saga = result.saga

                    // Update books with saga ID and order
                    val bookOrders = _sagaBooks.value.associate {
                        it.book.id to it.order
                    }

                    when (val orderResult = sagasApiService.updateBooksOrder(saga.id, bookOrders)) {
                        is ActionResult.Success -> {
                            _uiState.value = SagaUiState.Success("Saga created successfully")
                            _sagaBooks.value = emptyList()
                            loadSagas()
                        }
                        is ActionResult.Error -> {
                            _error.value = orderResult.message
                        }
                    }
                }
                is SagaResult.Error -> {
                    _error.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    fun updateSaga(
        sagaId: Long,
        name: String,
        description: String?,
        isCompleted: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // Update saga metadata
            when (val result = sagasApiService.updateSaga(
                sagaId = sagaId,
                name = name,
                description = description,
                primaryAuthorId = null, // Keep existing
                totalBooks = _sagaBooks.value.size,
                isCompleted = isCompleted
            )) {
                is SagaResult.Success -> {
                    // Update books order
                    val bookOrders = _sagaBooks.value.associate {
                        it.book.id to it.order
                    }

                    when (val orderResult = sagasApiService.updateBooksOrder(sagaId, bookOrders)) {
                        is ActionResult.Success -> {
                            _uiState.value = SagaUiState.Success("Saga updated successfully")
                            loadSagas()
                        }
                        is ActionResult.Error -> {
                            _error.value = orderResult.message
                        }
                    }
                }
                is SagaResult.Error -> {
                    _error.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    fun deleteSaga(sagaId: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = sagasApiService.deleteSaga(sagaId)) {
                is ActionResult.Success -> {
                    _uiState.value = SagaUiState.Success("Saga deleted successfully")
                    loadSagas()
                }
                is ActionResult.Error -> {
                    _error.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    fun cancelEditing() {
        _uiState.value = SagaUiState.SagasList
        _currentSaga.value = null
        _sagaBooks.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    fun resetUiState() {
        _uiState.value = SagaUiState.SagasList
    }
}