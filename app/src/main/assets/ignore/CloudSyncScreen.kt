package com.example.mybookhoard.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.BooksVm
import com.example.mybookhoard.api.AuthState
import com.example.mybookhoard.api.ConnectionState
import kotlinx.coroutines.launch

@Composable
fun CloudSyncScreen(
    vm: BooksVm,
    authState: AuthState.Authenticated,
    connectionState: ConnectionState
) {
    val books by vm.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf("") }

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
            CloudSyncStatusCard(
                connectionState = connectionState,
                bookCount = books.size,
                user = authState.user
            )

            // Action Cards
            when (connectionState) {
                is ConnectionState.Online -> {
                    OnlineActionsSection(
                        bookCount = books.size,
                        onSyncFromServer = {
                            scope.launch {
                                vm.syncFromServer()
                                snackbarHostState.showSnackbar("Synced from server successfully!")
                            }
                        },
                        onSyncToServer = {
                            scope.launch {
                                vm.syncToServer()
                                snackbarHostState.showSnackbar("Synced to server successfully!")
                            }
                        },
                        onSignOut = {
                            dialogAction = "signout"
                            showConfirmDialog = true
                        }
                    )
                }

                is ConnectionState.Syncing -> {
                    SyncingSection()
                }

                is ConnectionState.Error -> {
                    ErrorSection(
                        error = connectionState.message,
                        onRetry = {
                            scope.launch {
                                vm.syncFromServer()
                            }
                        }
                    )
                }

                is ConnectionState.Offline -> {
                    OfflineSection(
                        onRetry = {
                            scope.launch {
                                vm.syncFromServer()
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
                        "signout" -> "Sign Out?"
                        else -> "Confirm Action"
                    }
                )
            },
            text = {
                Text(
                    when (dialogAction) {
                        "signout" -> "This will sign you out and clear your local data. Your books will remain safely stored on the server."
                        else -> "Are you sure?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        when (dialogAction) {
                            "signout" -> {
                                scope.launch {
                                    vm.logout()
                                    snackbarHostState.showSnackbar("Signed out successfully")
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when (dialogAction) {
                            "signout" -> "Sign Out"
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
private fun CloudSyncStatusCard(
    connectionState: ConnectionState,
    bookCount: Int,
    user: com.example.mybookhoard.api.User
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
            val (icon, title, subtitle, color) = when (connectionState) {
                is ConnectionState.Online -> {
                    quadruple(
                        Icons.Default.Cloud,
                        "Connected",
                        "Synced with server",
                        MaterialTheme.colorScheme.primary
                    )
                }
                is ConnectionState.Syncing -> {
                    quadruple(
                        Icons.Default.CloudSync,
                        "Syncing...",
                        "Please wait",
                        MaterialTheme.colorScheme.primary
                    )
                }
                is ConnectionState.Error -> {
                    quadruple(
                        Icons.Default.CloudOff,
                        "Sync Error",
                        connectionState.message,
                        MaterialTheme.colorScheme.error
                    )
                }
                is ConnectionState.Offline -> {
                    quadruple(
                        Icons.Default.CloudOff,
                        "Offline",
                        "Working offline",
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                Text(
                    text = "Signed in as ${user.username} • $bookCount books",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnlineActionsSection(
    bookCount: Int,
    onSyncFromServer: () -> Unit,
    onSyncToServer: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Sync Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSyncFromServer,
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

            OutlinedButton(
                onClick = onSyncToServer,
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
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
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
            text = "Syncing with server...",
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
            Text("Retry Sync")
        }
    }
}

@Composable
private fun OfflineSection(
    onRetry: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Working Offline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your changes are saved locally and will sync when you're back online.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try to Connect")
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
                text = "About Cloud Sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Your library is automatically synced with our secure servers",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Download pulls the latest data from the server",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Upload pushes your local changes to the server",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• Your data is secured with industry-standard encryption",
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