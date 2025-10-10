package com.example.mybookhoard.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Rating selector component for personal book ratings
 * Allows selection from 0.0 to 10.0 with 0.1 increments
 * Path: app/src/main/java/com/example/mybookhoard/components/common/RatingSelector.kt
 */
@Composable
fun RatingSelector(
    currentRating: Float?,
    onRatingChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var sliderValue by remember(currentRating) {
        mutableFloatStateOf(currentRating ?: 0f)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Personal Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (sliderValue > 0f) String.format("%.1f", sliderValue) else "Not rated",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/10",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val roundedValue = (sliderValue * 10).roundToInt() / 10f
                onRatingChange(if (roundedValue > 0f) roundedValue else null)
            },
            valueRange = 0f..10f,
            steps = 99, // 0.1 increments = 100 steps - 1
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "5.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "10.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sliderValue > 0f && enabled) {
            OutlinedButton(
                onClick = {
                    sliderValue = 0f
                    onRatingChange(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Rating")
            }
        }
    }
}