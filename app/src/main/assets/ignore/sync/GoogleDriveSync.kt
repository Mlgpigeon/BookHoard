package ignore.sync

import android.content.Context
import android.content.Intent
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.forEach
import kotlin.collections.mapNotNull
import kotlin.io.byteInputStream
import kotlin.let
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.text.isNullOrBlank
import kotlin.text.trim

/**
 * Simplified Google Drive Sync using Google Sign-In only
 * This version focuses on authentication and basic sync state management
 * The actual Drive API integration can be added later once dependencies are resolved
 */
class SimplifiedGoogleDriveSync(private val context: android.content.Context) {

    private val _syncState =
        kotlinx.coroutines.flow.MutableStateFlow<SyncState>(SyncState.NotSignedIn)
    val syncState: kotlinx.coroutines.flow.StateFlow<SyncState> = _syncState

    private val _lastSyncTime = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val lastSyncTime: kotlinx.coroutines.flow.StateFlow<String?> = _lastSyncTime

    private var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private val prefs = context.getSharedPreferences("bookhoard_sync", android.content.Context.MODE_PRIVATE)

    init {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)

        // Load last sync time
        _lastSyncTime.value = prefs.getString("last_sync", null)

        // Check if already signed in
        com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            _syncState.value = SyncState.SignedIn(account.email ?: "Unknown")
        }
    }

    fun getSignInIntent(): android.content.Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (account != null) {
                    _syncState.value = SyncState.SignedIn(account.email ?: "Unknown")
                    true
                } else {
                    _syncState.value = SyncState.NotSignedIn
                    false
                }
            } catch (e: kotlin.Exception) {
                _syncState.value = SyncState.Error("Sign in failed: ${e.message}")
                false
            }
        }
    }

    suspend fun uploadBooks(books: List<com.example.mybookhoard.data.Book>): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                // Simulate upload delay
                kotlinx.coroutines.delay(2000)

                // For now, we'll just save to local storage as a mock
                // Later this can be replaced with actual Drive API calls
                val csvContent = booksToCSV(books)
                prefs.edit().putString("backup_csv", csvContent).apply()

                // Update last sync time
                val now =
                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date())
                _lastSyncTime.value = now
                prefs.edit().putString("last_sync", now).apply()

                _syncState.value = SyncState.SignedIn(getCurrentUserEmail())
                true

            } catch (e: kotlin.Exception) {
                _syncState.value = SyncState.Error("Upload failed: ${e.message}")
                false
            }
        }
    }

    suspend fun downloadBooks(): List<com.example.mybookhoard.data.Book>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                // Simulate download delay
                kotlinx.coroutines.delay(2000)

                // For now, read from local storage as a mock
                // Later this can be replaced with actual Drive API calls
                val csvContent = prefs.getString("backup_csv", null)
                    ?: throw kotlin.Exception("No backup found")

                val books = csvToBooks(csvContent)

                // Update last sync time
                val now =
                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date())
                _lastSyncTime.value = now
                prefs.edit().putString("last_sync", now).apply()

                _syncState.value = SyncState.SignedIn(getCurrentUserEmail())
                books

            } catch (e: kotlin.Exception) {
                _syncState.value = SyncState.Error("Download failed: ${e.message}")
                null
            }
        }
    }

    private fun booksToCSV(books: List<com.example.mybookhoard.data.Book>): String {
        val output = java.io.ByteArrayOutputStream()
        com.github.doyaaaaaken.kotlincsv.dsl.csvWriter().open(output) {
            // Header
            com.github.doyaaaaaken.kotlincsv.client.ICsvFileWriter.writeRow(
                kotlin.collections.listOf(
                    "Title", "Author", "Saga", "Description",
                    "Status", "Wishlist", "DateAdded"
                )
            )

            // Data
            books.forEach { book ->
                com.github.doyaaaaaken.kotlincsv.client.ICsvFileWriter.writeRow(
                    kotlin.collections.listOf(
                        book.title,
                        book.author ?: "",
                        book.saga ?: "",
                        book.description ?: "",
                        book.status.name,
                        book.wishlist?.name ?: "",
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    )
                )
            }
        }
        return output.toString()
    }

    private fun csvToBooks(csvContent: String): List<com.example.mybookhoard.data.Book> {
        val reader = com.github.doyaaaaaken.kotlincsv.dsl.csvReader {
            com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext.skipEmptyLine = true
            com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext.autoRenameDuplicateHeaders =
                true
        }
        val rows = reader.readAllWithHeader(csvContent.byteInputStream())

        return rows.mapNotNull { row ->
            val title = row["Title"]?.trim() ?: return@mapNotNull null
            if (title.isBlank()) return@mapNotNull null

            val author = row["Author"]?.trim()?.ifBlank { null }
            val saga = row["Saga"]?.trim()?.ifBlank { null }
            val description = row["Description"]?.trim()?.ifBlank { null }

            val status = try {
                com.example.mybookhoard.data.ReadingStatus.valueOf(row["Status"] ?: "NOT_STARTED")
            } catch (e: kotlin.Exception) {
                com.example.mybookhoard.data.ReadingStatus.NOT_STARTED
            }

            val wishlist = try {
                val wishlistStr = row["Wishlist"]?.trim()
                if (wishlistStr.isNullOrBlank()) null
                else com.example.mybookhoard.data.WishlistStatus.valueOf(wishlistStr)
            } catch (e: kotlin.Exception) {
                null
            }

            com.example.mybookhoard.data.Book(
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
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            googleSignInClient.signOut()
            _syncState.value = SyncState.NotSignedIn
            _lastSyncTime.value = null
            prefs.edit().remove("last_sync").apply()
        }
    }

    private fun getCurrentUserEmail(): String {
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email ?: "Unknown"
    }

    fun isSignedIn(): Boolean {
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context) != null
    }
}

sealed class SyncState {
    object NotSignedIn : SyncState()
    data class SignedIn(val email: String) : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}