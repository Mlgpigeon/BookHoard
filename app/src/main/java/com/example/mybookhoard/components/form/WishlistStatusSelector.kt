package com.example.mybookhoard.components.form

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

/**
 * Dropdown selector for wishlist status when creating a book
 * Allows user to optionally create a UserBook with wishlist status
 */
@Composable
fun WishlistStatusSelector(
    selectedStatus: UserBookWishlistStatus?,
    onStatusChange: (UserBookWishlistStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Add to Collection (Optional)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (selectedStatus) {
                        UserBookWishlistStatus.WISH -> "⭐ Wishlist"
                        UserBookWishlistStatus.ON_THE_WAY -> "📦 On the Way"
                        UserBookWishlistStatus.OBTAINED -> "📚 Obtained"
                        null -> "Don't add to collection yet"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (selectedStatus != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Don't add to collection") },
                onClick = {
                    onStatusChange(null)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text("⭐ Add to Wishlist") },
                onClick = {
                    onStatusChange(UserBookWishlistStatus.WISH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("📦 Mark as On the Way") },
                onClick = {
                    onStatusChange(UserBookWishlistStatus.ON_THE_WAY)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("📚 Mark as Obtained") },
                onClick = {
                    onStatusChange(UserBookWishlistStatus.OBTAINED)
                    expanded = false
                }
            )
        }

        if (selectedStatus != null) {
            Text(
                text = "The book will be added to your collection with this status",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}