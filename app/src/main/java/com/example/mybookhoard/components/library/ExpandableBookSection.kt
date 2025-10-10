// File: app/src/main/java/com/example/mybookhoard/components/library/ExpandableBookSection.kt

package com.example.mybookhoard.components.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.entities.*

@Composable
fun ExpandableBookSection(
    title: String,
    books: List<BookWithUserDataExtended>,
    onReadingStatusChange: (Long, UserBookReadingStatus) -> Unit,
    onWishlistStatusChange: (Long, UserBookWishlistStatus) -> Unit,
    onRemoveFromCollection: (Long) -> Unit,
    onBookClick: (BookWithUserDataExtended) -> Unit = {},
    showReadingStatusButton: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        // Section header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (backgroundColor != Color.Unspecified)
                    backgroundColor
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = books.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
        }

        // Books list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (books.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No books in this category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    books.forEach { bookWithUserData ->
                        LibraryBookCard(
                            bookWithUserData = bookWithUserData,
                            onReadingStatusChange = { newStatus ->
                                onReadingStatusChange(bookWithUserData.book.id, newStatus)
                            },
                            onWishlistStatusChange = { newStatus ->
                                onWishlistStatusChange(bookWithUserData.book.id, newStatus)
                            },
                            onRemoveFromCollection = {
                                onRemoveFromCollection(bookWithUserData.book.id)
                            },
                            onBookClick = {
                                onBookClick(bookWithUserData)
                            },
                            showReadingStatusButton = showReadingStatusButton
                        )
                    }
                }
            }
        }
    }
}