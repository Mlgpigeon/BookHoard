// File: app/src/main/java/com/example/mybookhoard/ui/components/search/SearchDialogs.kt
// Usando SearchResult real del proyecto

package com.example.mybookhoard.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.WishlistStatus
import com.example.mybookhoard.api.SearchResult

@Composable
fun AddGoogleBookDialog(
    searchResult: SearchResult,
    onDismiss: () -> Unit,
    onConfirm: (WishlistStatus) -> Unit, // Keep existing signature for compatibility
    modifier: Modifier = Modifier
) {
    var selectedWishlistStatus by remember { mutableStateOf(WishlistStatus.WISH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Library") },
        text = {
            Column {
                Text(
                    text = "Add \"${searchResult.title}\" to your library?",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!searchResult.author.isNullOrBlank()) {
                    Text(
                        text = "by ${searchResult.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "Wishlist Status:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )

                WishlistStatus.entries.forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedWishlistStatus == status,
                            onClick = { selectedWishlistStatus = status }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (status) {
                                WishlistStatus.WISH -> "Wishlist"
                                WishlistStatus.ON_THE_WAY -> "On the way"
                                WishlistStatus.OBTAINED -> "Obtained"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedWishlistStatus) }) {
                Text("Add to Library")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}