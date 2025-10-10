package com.example.mybookhoard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybookhoard.components.form.*
import com.example.mybookhoard.viewmodels.AddBookViewModel
import com.example.mybookhoard.components.form.BookImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    onNavigateBack: () -> Unit,
    onBookCreated: () -> Unit,
    addBookViewModel: AddBookViewModel = viewModel()
) {
    val uiState by addBookViewModel.uiState.collectAsState()
    val formState by addBookViewModel.formState.collectAsState()
    val authorSuggestions by addBookViewModel.authorSuggestions.collectAsState()
    val sagaSuggestions by addBookViewModel.sagaSuggestions.collectAsState()
    val sagaNumber by addBookViewModel.sagaNumber.collectAsState()
    val selectedWishlistStatus by addBookViewModel.selectedWishlistStatus.collectAsState()
    val selectedImageUri by addBookViewModel.selectedImageUri.collectAsState()

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Book") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Create a new book entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Form Fields
            BookFormField(
                value = formState.title,
                onValueChange = { addBookViewModel.updateTitle(it) },
                label = "Title",
                leadingIcon = Icons.Default.Title,
                modifier = Modifier.fillMaxWidth(),
                isRequired = true,
                isError = formState.titleError != null,
                supportingText = formState.titleError
            )

            AuthorFieldWithSuggestions(
                author = formState.author,
                onAuthorChange = { addBookViewModel.updateAuthor(it) },
                suggestions = authorSuggestions,
                onSuggestionClick = { addBookViewModel.selectAuthorSuggestion(it) },
                isError = formState.authorError != null,
                errorMessage = formState.authorError,
                modifier = Modifier.fillMaxWidth()
            )

            SagaFieldWithSuggestions(
                sagaName = formState.saga,
                onSagaNameChange = { addBookViewModel.updateSaga(it) },
                sagaNumber = sagaNumber,
                onSagaNumberChange = { addBookViewModel.updateSagaNumber(it) },
                suggestions = sagaSuggestions,
                onSuggestionClick = { addBookViewModel.selectSagaSuggestion(it) },
                modifier = Modifier.fillMaxWidth()
            )

            BookImagePicker(
                selectedImageUri = selectedImageUri,
                onImageSelected = { uri -> addBookViewModel.updateImageUri(uri) },
                onImageRemoved = { addBookViewModel.updateImageUri(null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            BookFormField(
                value = formState.description,
                onValueChange = { addBookViewModel.updateDescription(it) },
                label = "Description",
                leadingIcon = Icons.Default.Description,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                supportingText = "Brief description or summary of the book"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BookFormField(
                    value = formState.publicationYear,
                    onValueChange = { addBookViewModel.updatePublicationYear(it) },
                    label = "Year",
                    leadingIcon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    isError = formState.publicationYearError != null,
                    supportingText = formState.publicationYearError
                )

                LanguageDropdown(
                    selectedLanguage = formState.language,
                    onLanguageChange = { addBookViewModel.updateLanguage(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            BookFormField(
                value = formState.isbn,
                onValueChange = { addBookViewModel.updateIsbn(it) },
                label = "ISBN",
                leadingIcon = Icons.Default.Numbers,
                modifier = Modifier.fillMaxWidth(),
                isError = formState.isbnError != null,
                supportingText = formState.isbnError ?: "10 or 13 digit ISBN (optional)"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // NEW: Wishlist Status Selector
            WishlistStatusSelector(
                selectedStatus = selectedWishlistStatus,
                onStatusChange = { addBookViewModel.updateWishlistStatus(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = {
                    addBookViewModel.createBook()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = formState.isValid() && uiState !is AddBookViewModel.AddBookUiState.Loading
            ) {
                when (uiState) {
                    is AddBookViewModel.AddBookUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Book...")
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Book",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddBookViewModel.AddBookUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Book '${state.bookTitle}' created successfully!",
                    duration = SnackbarDuration.Short
                )
                onBookCreated()
            }
            is AddBookViewModel.AddBookUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${state.message}",
                    duration = SnackbarDuration.Long
                )
            }
            else -> { /* No action needed */ }
        }
    }
}