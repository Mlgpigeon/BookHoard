package com.example.mybookhoard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybookhoard.data.ReadingStatus

@Composable
fun ReadingStatusSelector(
    currentStatus: ReadingStatus,
    onStatusChange: (ReadingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Reading Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusButton(
                status = ReadingStatus.NOT_STARTED,
                currentStatus = currentStatus,
                onClick = onStatusChange,
                modifier = Modifier.weight(1f)
            )

            StatusButton(
                status = ReadingStatus.READING,
                currentStatus = currentStatus,
                onClick = onStatusChange,
                modifier = Modifier.weight(1f)
            )

            StatusButton(
                status = ReadingStatus.READ,
                currentStatus = currentStatus,
                onClick = onStatusChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusButton(
    status: ReadingStatus,
    currentStatus: ReadingStatus,
    onClick: (ReadingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentStatus == status
    val (icon, label, color) = when (status) {
        ReadingStatus.NOT_STARTED -> Triple(
            Icons.Default.RadioButtonUnchecked,
            "Unread",
            MaterialTheme.colorScheme.outline
        )
        ReadingStatus.READING -> Triple(
            Icons.Default.MenuBook,
            "Reading",
            MaterialTheme.colorScheme.primary
        )
        ReadingStatus.READ -> Triple(
            Icons.Default.CheckCircle,
            "Read",
            MaterialTheme.colorScheme.tertiary
        )
    }

    OutlinedButton(
        onClick = { onClick(status) },
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, color)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) {
                color.copy(alpha = 0.1f)
            } else {
                Color.Transparent
            },
            contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}