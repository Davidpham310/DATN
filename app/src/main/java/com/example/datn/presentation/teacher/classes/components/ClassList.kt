package com.example.datn.presentation.teacher.classes.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.datn.domain.models.Class

@Composable
fun ClassList(
    classes: List<Class>,
    onEdit: (Class) -> Unit,
    onDelete: (Class) -> Unit,
    onClick: (Class) -> Unit,
    onManageEnrollment: ((Class) -> Unit)? = null,
    onViewMembers: ((Class) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(classes) { classObj ->
            ClassItem(
                classObj = classObj,
                onEdit = { onEdit(classObj) },
                onClick = { onClick(classObj) },
                onDelete = { onDelete(classObj) },
                onManageEnrollment = onManageEnrollment?.let { { it(classObj) } },
                onViewMembers = onViewMembers?.let { { it(classObj) } }
            )
        }
    }
}
