package com.example.datn.presentation.teacher.classes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Class

@Composable
fun ClassItem(
    classObj: Class,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = classObj.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Mã lớp: ${classObj.classCode}", style = MaterialTheme.typography.bodyMedium)
            }

            Row {
                TextButton(onClick = onEdit) {
                    Text("Sửa")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDelete) {
                    Text("Xóa")
                }
            }
        }
    }
}
