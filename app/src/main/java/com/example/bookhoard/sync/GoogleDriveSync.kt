package com.example.bookhoard.sync

import android.content.Context
import android.content.Intent
import com.example.bookhoard.data.Book
import com.example.bookhoard.data.ReadingStatus
import com.example.bookhoard.data.WishlistStatus
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simplified Google Drive Sync using Google Sign-In only
 * This version focuses on authentication and basic sync state management
 * The actual Drive API integration can be added later once dependencies are resolved
 */
class SimplifiedGoogleDriveSync(private val context: Context) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.NotSignedIn)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private var googleSignInClient: GoogleSignInClient

    private val prefs = context.getSharedPreferences("bookhoard_sync", Context.MODE_PRIVATE)

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Load last sync time
        _lastSyncTime.value = prefs.getString("last_sync", null)

        // Check if already signed in
        GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            _syncState.value = SyncState.SignedIn(account.email ?: "Unknown")
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (account != null) {
                    _syncState.value = SyncState.SignedIn(account.email ?: "Unknown")
                    true
                } else {
                    _syncState.value = SyncState.NotSignedIn
                    false
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Sign in failed: ${e.message}")
                false
            }
        }
    }

    suspend fun uploadBooks(books: List<Book>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                // Simulate upload delay
                kotlinx.coroutines.delay(2000)

                // For now, we'll just save to local storage as a mock
                // Later this can be replaced with actual Drive API calls
                val csvContent = booksToCSV(books)
                prefs.edit().putString("backup_csv", csvContent).apply()

                // Update last sync time
                val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                _lastSyncTime.value = now
                prefs.edit().putString("last_sync", now).apply()

                _syncState.value = SyncState.SignedIn(getCurrentUserEmail())
                true

            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Upload failed: ${e.message}")
                false
            }
        }
    }

    suspend fun downloadBooks(): List<Book>? {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                // Simulate download delay
                kotlinx.coroutines.delay(2000)

                // For now, read from local storage as a mock
                // Later this can be replaced with actual Drive API calls
                val csvContent = prefs.getString("backup_csv", null)
                    ?: throw Exception("No backup found")

                val books = csvToBooks(csvContent)

                // Update last sync time
                val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                _lastSyncTime.value = now
                prefs.edit().putString("last_sync", now).apply()

                _syncState.value = SyncState.SignedIn(getCurrentUserEmail())
                books

            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Download failed: ${e.message}")
                null
            }
        }
    }

    private fun booksToCSV(books: List<Book>): String {
        val output = ByteArrayOutputStream()
        csvWriter().open(output) {
            // Header
            writeRow(listOf(
                "Title", "Author", "Saga", "Description",
                "Status", "Wishlist", "DateAdded"
            ))

            // Data
            books.forEach { book ->
                writeRow(listOf(
                    book.title,
                    book.author ?: "",
                    book.saga ?: "",
                    book.description ?: "",
                    book.status.name,
                    book.wishlist?.name ?: "",
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                ))
            }
        }
        return output.toString()
    }

    private fun csvToBooks(csvContent: String): List<Book> {
        val reader = csvReader {
            skipEmptyLine = true
            autoRenameDuplicateHeaders = true
        }
        val rows = reader.readAllWithHeader(csvContent.byteInputStream())

        return rows.mapNotNull { row ->
            val title = row["Title"]?.trim() ?: return@mapNotNull null
            if (title.isBlank()) return@mapNotNull null

            val author = row["Author"]?.trim()?.ifBlank { null }
            val saga = row["Saga"]?.trim()?.ifBlank { null }
            val description = row["Description"]?.trim()?.ifBlank { null }

            val status = try {
                ReadingStatus.valueOf(row["Status"] ?: "NOT_STARTED")
            } catch (e: Exception) {
                ReadingStatus.NOT_STARTED
            }

            val wishlist = try {
                val wishlistStr = row["Wishlist"]?.trim()
                if (wishlistStr.isNullOrBlank()) null
                else WishlistStatus.valueOf(wishlistStr)
            } catch (e: Exception) {
                null
            }

            Book(
                title = title,
                author = author,
                saga = saga,
                description = description,
                status = status,
                wishlist = wishlist
            )
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.Main) {
            googleSignInClient.signOut()
            _syncState.value = SyncState.NotSignedIn
            _lastSyncTime.value = null
            prefs.edit().remove("last_sync").apply()
        }
    }

    private fun getCurrentUserEmail(): String {
        return GoogleSignIn.getLastSignedInAccount(context)?.email ?: "Unknown"
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }
}

sealed class SyncState {
    object NotSignedIn : SyncState()
    data class SignedIn(val email: String) : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}