package com.example.mybookhoard.components.sagas

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.mybookhoard.viewmodels.SagasViewModel

/**
 * Draggable and reorderable list of books in saga
 * Uses long press to start dragging
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableBooksList(
    books: List<SagasViewModel.BookWithOrder>,
    onRemoveBook: (Long) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedItem by remember { mutableStateOf<Int?>(null) }
    var targetItem by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = books,
            key = { _, book -> book.book.id }
        ) { index, bookWithOrder ->
            val isDragging = draggedItem == index
            val isTarget = targetItem == index

            DraggableBookItem(
                book = bookWithOrder.book,
                order = bookWithOrder.order,
                isDragging = isDragging,
                isTarget = isTarget,
                onRemove = { onRemoveBook(bookWithOrder.book.id) },
                onDragStart = { draggedItem = index },
                onDragEnd = {
                    draggedItem?.let { from ->
                        targetItem?.let { to ->
                            if (from != to) {
                                onReorder(from, to)
                            }
                        }
                    }
                    draggedItem = null
                    targetItem = null
                },
                onDragOver = { targetItem = index },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            )
        }
    }
}

/**
 * Individual book item that can be dragged
 */
@Composable
private fun DraggableBookItem(
    book: com.example.mybookhoard.data.entities.Book,
    order: Int,
    isDragging: Boolean,
    isTarget: Boolean,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        label = "elevation"
    )

    val scale by remember { derivedStateOf { if (isDragging) 1.05f else 1f } }

    ElevatedCard(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, RoundedCornerShape(12.dp))
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { _, _ -> onDragOver() }
                )
            },
        colors = if (isTarget) CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) else CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Long press to drag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            // Order number
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = order.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Book info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove book"
                )
            }
        }
    }
}