// ============================================================================
// ARCHIVO COMPLETO: app/src/main/java/com/example/mybookhoard/components/library/LibraryBookCard.kt
// Cambios marcados con ✅ NUEVO o ✅ CAMBIADO
// ============================================================================

package com.example.mybookhoard.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape  // ✅ NUEVO
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip  // ✅ NUEVO
import androidx.compose.ui.layout.ContentScale  // ✅ NUEVO
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage  // ✅ NUEVO
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.components.dialogs.RemoveBookDialog
import com.example.mybookhoard.components.dialogs.WishlistSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBookCard(
    bookWithUserData: BookWithUserDataExtended,
    onReadingStatusChange: (UserBookReadingStatus) -> Unit,
    onWishlistStatusChange: (UserBookWishlistStatus) -> Unit,
    onRemoveFromCollection: () -> Unit,
    onBookClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showReadingStatusButton: Boolean = true
) {
    var showActionsMenu by remember { mutableStateOf(false) }
    var showWishlistDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    val book = bookWithUserData.book
    val userBook = bookWithUserData.userBook

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onBookClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            userBook?.personalRating?.let { rating ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format("%.1f/10", rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ============================================================================
            // ✅ CAMBIADO: Este Row ahora usa spacedBy en lugar de SpaceBetween
            // ============================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),  // ✅ CAMBIADO de SpaceBetween
                verticalAlignment = Alignment.Top
            ) {
                // ============================================================================
                // ✅ NUEVO: Bloque completo para mostrar la imagen de portada
                // ============================================================================
                if (!book.coverSelected.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.coverSelected)
                            .crossfade(true)
                            .size(120, 180) // Tamaño específico para optimizar memoria
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Cover of ${book.title}",
                        modifier = Modifier
                            .width(60.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                // ============================================================================
                // FIN DEL BLOQUE NUEVO
                // ============================================================================

                // Book info (Este bloque NO cambia, solo se desplaza a la derecha si hay imagen)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!bookWithUserData.authorName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "by ${bookWithUserData.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Book metadata
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (book.publicationYear != null) {
                            BookMetadataChip(text = book.publicationYear.toString())
                        }
                        if (book.language.isNotBlank() && book.language != "en") {
                            BookMetadataChip(text = book.language.uppercase())
                        }
                    }
                }

                // Action button (NO cambia)
                Box {
                    IconButton(
                        onClick = { showActionsMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions"
                        )
                    }

                    DropdownMenu(
                        expanded = showActionsMenu,
                        onDismissRequest = { showActionsMenu = false }
                    ) {
                        if (showReadingStatusButton) {
                            // Reading status options
                            DropdownMenuItem(
                                text = { Text("Mark as Reading") },
                                onClick = {
                                    onReadingStatusChange(UserBookReadingStatus.READING)
                                    showActionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Mark as Read") },
                                onClick = {
                                    onReadingStatusChange(UserBookReadingStatus.READ)
                                    showActionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Mark as Not Started") },
                                onClick = {
                                    onReadingStatusChange(UserBookReadingStatus.NOT_STARTED)
                                    showActionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )

                            HorizontalDivider()
                        }

                        // Wishlist status
                        DropdownMenuItem(
                            text = { Text("Change Wishlist Status") },
                            onClick = {
                                showWishlistDialog = true
                                showActionsMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )

                        HorizontalDivider()

                        // Remove from collection
                        DropdownMenuItem(
                            text = { Text("Remove from Collection") },
                            onClick = {
                                showRemoveDialog = true
                                showActionsMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs (NO cambian)
    if (showWishlistDialog) {
        WishlistSelectionDialog(
            onDismiss = { showWishlistDialog = false },
            onStatusSelected = { status ->
                onWishlistStatusChange(status)
                showWishlistDialog = false
            }
        )
    }

    if (showRemoveDialog) {
        RemoveBookDialog(
            bookTitle = book.title,
            onConfirm = {
                onRemoveFromCollection()
                showRemoveDialog = false
            },
            onDismiss = { showRemoveDialog = false }
        )
    }
}

// Componentes auxiliares (NO cambian)
@Composable
private fun BookMetadataChip(
    text: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier
    )
}

@Composable
private fun StatusChip(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}