// File: app/src/main/java/com/example/mybookhoard/ui/components/search/SearchResultsHeader.kt

package com.example.mybookhoard.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SearchResultsHeader(
    searchQuery: String,
    totalResults: Int,
    localCount: Int,
    googleCount: Int,
    isSearchingGoogle: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Search query
            Text(
                text = "Results for \"$searchQuery\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Results summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total results
                Text(
                    text = "$totalResults total results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Local library indicator
                if (localCount > 0) {
                    SourceChip(
                        text = "$localCount in library",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Google Books indicator
                when {
                    isSearchingGoogle -> {
                        SourceChip(
                            text = "Searching online...",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    googleCount > 0 -> {
                        SourceChip(
                            text = "$googleCount online",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}