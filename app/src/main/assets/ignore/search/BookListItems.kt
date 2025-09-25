// File: app/src/main/java/com/example/mybookhoard/ui/components/search/BookListItems.kt
// Solo usando propiedades reales de SearchResult

package com.example.mybookhoard.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus
import com.example.mybookhoard.api.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBookItem(
    searchResult: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = searchResult.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Author
                searchResult.author?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Saga
                searchResult.saga?.let { saga ->
                    Text(
                        text = saga,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Status chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // Source chip
                    SourceChip(
                        text = searchResult.sourceLabel,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Reading status
                    val parsedStatus = runCatching {
                        searchResult.status?.let { ReadingStatus.valueOf(it) }
                    }.getOrNull()

                    parsedStatus?.let { status ->
                        StatusChip(
                            text = when (status) {
                                ReadingStatus.NOT_STARTED -> "To Read"
                                ReadingStatus.READING -> "Reading"
                                ReadingStatus.READ -> "Read"
                            },
                            color = when (status) {
                                ReadingStatus.NOT_STARTED -> MaterialTheme.colorScheme.outline
                                ReadingStatus.READING -> MaterialTheme.colorScheme.tertiary
                                ReadingStatus.READ -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    // Wishlist status
                    val parsedWishlist = runCatching {
                        searchResult.wishlist?.let { WishlistStatus.valueOf(it) }
                    }.getOrNull()

                    parsedWishlist?.takeIf { it != WishlistStatus.OBTAINED }?.let { wishlistStatus ->
                        StatusChip(
                            text = when (wishlistStatus) {
                                WishlistStatus.WISH -> "Wishlist"
                                WishlistStatus.ON_THE_WAY -> "On the way"
                                WishlistStatus.OBTAINED -> "Obtained"
                            },
                            color = when (wishlistStatus) {
                                WishlistStatus.WISH -> MaterialTheme.colorScheme.secondary
                                WishlistStatus.ON_THE_WAY -> MaterialTheme.colorScheme.tertiary
                                WishlistStatus.OBTAINED -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleBookItem(
    searchResult: SearchResult,
    onClick: () -> Unit,
    onAddToLibrary: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = searchResult.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Author
                searchResult.author?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Saga
                searchResult.saga?.let { saga ->
                    Text(
                        text = saga,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Description preview
                searchResult.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Source chip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    SourceChip(
                        text = searchResult.sourceLabel,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Add to library button
            OutlinedButton(
                onClick = { onAddToLibrary(searchResult) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}