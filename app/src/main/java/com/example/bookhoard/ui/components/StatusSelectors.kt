package com.example.bookhoard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookhoard.data.WishlistStatus

@Composable
fun WishlistDropdownSelector(
    wishlistStatus: WishlistStatus?,
    onStatusChange: (WishlistStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Wishlist Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (wishlistStatus) {
                        WishlistStatus.WISH -> "⭐ Wish"
                        WishlistStatus.ON_THE_WAY -> "📦 On the way"
                        WishlistStatus.OBTAINED -> "📚 Obtained"
                        null -> "Select wishlist status (optional)"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No wishlist status") },
                    onClick = {
                        onStatusChange(null)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("⭐ Wish") },
                    onClick = {
                        onStatusChange(WishlistStatus.WISH)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📦 On the way") },
                    onClick = {
                        onStatusChange(WishlistStatus.ON_THE_WAY)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("📚 Obtained") },
                    onClick = {
                        onStatusChange(WishlistStatus.OBTAINED)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun WishlistChipSelector(
    wishlistStatus: WishlistStatus?,
    onStatusChange: (WishlistStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Wishlist Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val options = listOf(
            null to "None",
            WishlistStatus.WISH to "⭐ Wish",
            WishlistStatus.ON_THE_WAY to "📦 On the way",
            WishlistStatus.OBTAINED to "📚 Obtained"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (status, label) ->
                val isSelected = wishlistStatus == status

                FilterChip(
                    onClick = { onStatusChange(status) },
                    label = { Text(label) },
                    selected = isSelected,
                    modifier = Modifier.fillMaxWidth(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
    }
}