package com.example.mybookhoard.components.form

/**
* State model for book creation/editing form
* Path: app/src/main/java/com/example/mybookhoard/ui/components/form/BookFormState.kt
*/
data class BookFormState(
    val title: String = "",
    val author: String = "",
    val saga: String = "",
    val description: String = "",
    val publicationYear: String = "",
    val language: String = "en",
    val isbn: String = "",

    // Validation errors
    val titleError: String? = null,
    val authorError: String? = null,
    val sagaError: String? = null,
    val publicationYearError: String? = null,
    val isbnError: String? = null
)

/**
 * Validation extension for BookFormState
 */
fun BookFormState.isValid(): Boolean {
    return title.isNotBlank() &&
            titleError == null &&
            authorError == null &&
            sagaError == null &&
            publicationYearError == null &&
            isbnError == null
}

/**
 * Get validation errors for current state
 */
fun BookFormState.validate(): BookFormState {
    return this.copy(
        titleError = when {
            title.isBlank() -> "Title is required"
            title.length < 2 -> "Title must be at least 2 characters"
            else -> null
        },
        authorError = when {
            author.isNotBlank() && author.length < 2 -> "Author name must be at least 2 characters"
            else -> null
        },
        sagaError = when {
            saga.isNotBlank() && saga.length < 2 -> "Saga name must be at least 2 characters"
            else -> null
        },
        publicationYearError = when {
            publicationYear.isNotBlank() -> {
                val year = publicationYear.toIntOrNull()
                when {
                    year == null -> "Please enter a valid year"
                    year < 1000 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1 ->
                        "Please enter a year between 1000 and ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1}"
                    else -> null
                }
            }
            else -> null
        },
        isbnError = when {
            isbn.isNotBlank() -> {
                val cleanIsbn = isbn.replace("-", "").replace(" ", "")
                when {
                    cleanIsbn.length != 10 && cleanIsbn.length != 13 -> "ISBN must be 10 or 13 digits"
                    !cleanIsbn.all { it.isDigit() || (cleanIsbn.length == 10 && it.uppercaseChar() == 'X' && cleanIsbn.endsWith(it.toString())) } -> "ISBN contains invalid characters"
                    else -> null
                }
            }
            else -> null
        }
    )
}