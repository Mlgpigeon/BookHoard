package com.example.mybookhoard.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.sync.SimplifiedGoogleDriveSync
import com.example.mybookhoard.sync.SyncState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(
    vm: BooksVm,
    googleDriveSync: SimplifiedGoogleDriveSync
) {
    val syncStateValue by googleDriveSync.syncState.collectAsState()
    val lastSyncTime by googleDriveSync.lastSyncTime.collectAsState()
    val books by vm.items.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf("") }

    // Create local variable for smart cast
    val syncState = syncStateValue

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val success = googleDriveSync.handleSignInResult(account)

                if (success) {
                    snackbarHostState.showSnackbar("Successfully signed in!")
                } else {
                    snackbarHostState.showSnackbar("Sign in failed")
                }
            } catch (e: ApiException) {
                snackbarHostState.showSnackbar("Sign in error: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Header
            Text(
                text = "Cloud Sync",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Status Card
            SyncStatusCard(
                syncState = syncState,
                lastSyncTime = lastSyncTime,
                bookCount = books.size
            )

            when (syncState) {
                is SyncState.NotSignedIn -> {
                    NotSignedInSection(
                        onSignIn = {
                            signInLauncher.launch(googleDriveSync.getSignInIntent())
                        }
                    )
                }

                is SyncState.SignedIn -> {
                    SignedInSection(
                        email = syncState.email,
                        bookCount = books.size,
                        onUpload = {
                            dialogAction = "upload"
                            showConfirmDialog = true
                        },
                        onDownload = {
                            dialogAction = "download"
                            showConfirmDialog = true
                        },
                        onSignOut = {
                            scope.launch {
                                googleDriveSync.signOut()
                                snackbarHostState.showSnackbar("Signed out successfully")
                            }
                        }
                    )
                }

                is SyncState.Syncing -> {
                    SyncingSection()
                }

                is SyncState.Error -> {
                    ErrorSection(
                        error = syncState.message,
                        onRetry = {
                            if (googleDriveSync.isSignedIn()) {
                                // Already signed in, just retry the operation
                            } else {
                                signInLauncher.launch(googleDriveSync.getSignInIntent())
                            }
                        }
                    )
                }
            }

            // Info Section
            CloudSyncInfoSection()
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    when (dialogAction) {
                        "upload" -> "Upload to Cloud?"
                        "download" -> "Download from Cloud?"
                        else -> "Confirm Action"
                    }
                )
            },
            text = {
                Text(
                    when (dialogAction) {
                        "upload" -> "This will upload your current ${books.size} books to Google Drive, overwriting any existing backup."
                        "download" -> "This will download books from Google Drive and replace your current library. Your current ${books.size} books will be lost."
                        else -> "Are you sure?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            when (dialogAction) {
                                "upload" -> {
                                    val success = googleDriveSync.uploadBooks(books)
                                    snackbarHostState.showSnackbar(
                                        if (success) "Books uploaded successfully!"
                                        else "Upload failed"
                                    )
                                }
                                "download" -> {
                                    val downloadedBooks = googleDriveSync.downloadBooks()
                                    if (downloadedBooks != null) {
                                        vm.replaceAll(downloadedBooks)
                                        snackbarHostState.showSnackbar(
                                            "Downloaded ${downloadedBooks.size} books successfully!"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("Download failed")
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when (dialogAction) {
                            "upload" -> "Upload"
                            "download" -> "Download"
                            else -> "Confirm"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SyncStatusCard(
    syncState: SyncState,
    lastSyncTime: String?,
    bookCount: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, title, subtitle, color) = when (syncState) {
                is SyncState.NotSignedIn -> {
                    val icon = Icons.Default.CloudOff
                    val title = "Not Connected"
                    val subtitle = "Sign in to enable cloud sync"
                    val color = MaterialTheme.colorScheme.onSurfaceVariant
                    quadruple(icon, title, subtitle, color)
                }
                is SyncState.SignedIn -> {
                    val icon = Icons.Default.Cloud
                    val title = "Connected"
                    val subtitle = lastSyncTime?.let { "Last sync: $it" } ?: "Ready to sync"
                    val color = MaterialTheme.colorScheme.primary
                    quadruple(icon, title, subtitle, color)
                }
                is SyncState.Syncing -> {
                    val icon = Icons.Default.CloudSync
                    val title = "Syncing..."
                    val subtitle = "Please wait"
                    val color = MaterialTheme.colorScheme.primary
                    quadruple(icon, title, subtitle, color)
                }
                is SyncState.Error -> {
                    val icon = Icons.Default.Warning
                    val title = "Sync Error"
                    val subtitle = syncState.message
                    val color = MaterialTheme.colorScheme.error
                    quadruple(icon, title, subtitle, color)
                }
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (syncState is SyncState.SignedIn) {
                    Text(
                        text = "$bookCount books in library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotSignedInSection(
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Sign in with Google")
        }
    }
}

@Composable
private fun SignedInSection(
    email: String,
    bookCount: Int,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Signed in as",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onUpload,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Upload")
            }

            OutlinedButton(
                onClick = onDownload,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Download")
            }
        }

        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun SyncingSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Syncing with Google Drive...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorSection(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sync Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun CloudSyncInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "About Cloud Sync (Simplified Version)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Your library is saved locally as backup (Drive API integration pending)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Upload creates a CSV backup of your current books",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Download restores your library from the latest backup",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Google authentication is fully functional",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper function for tuple-like behavior
private fun <A, B, C, D> quadruple(a: A, b: B, c: C, d: D): Quadruple<A, B, C, D> {
    return Quadruple(a, b, c, d)
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)