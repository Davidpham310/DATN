package com.example.datn.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Date header for grouping messages by date
 * Shows sticky headers in chat screen
 */
@Composable
fun DateHeader(
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatDate(date),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

/**
 * Format date for display
 * - "Hôm nay" for today
 * - "Hôm qua" for yesterday
 * - "Thứ Hai, 04/11" for this week
 * - "04/11/2024" for older
 */
private fun formatDate(date: LocalDate): String {
    val today = LocalDate.now()
    val diff = ChronoUnit.DAYS.between(date, today)

    return when {
        diff == 0L -> "Hôm nay"
        diff == 1L -> "Hôm qua"
        diff < 7 -> {
            // This week - show day name and date
            val dayName = date.format(DateTimeFormatter.ofPattern("EEEE"))
            val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM"))
            "$dayName, $dateStr"
        }
        date.year == today.year -> {
            // This year - show date without year
            date.format(DateTimeFormatter.ofPattern("dd/MM"))
        }
        else -> {
            // Other years - show full date
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }
}

/**
 * Helper extension to convert Instant to LocalDate
 */
fun Instant.toLocalDate(): LocalDate {
    return this.atZone(ZoneId.systemDefault()).toLocalDate()
}
