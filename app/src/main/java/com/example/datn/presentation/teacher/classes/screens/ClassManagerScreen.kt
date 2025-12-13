package com.example.datn.presentation.teacher.classes.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.classmanager.ClassManagerEvent
import com.example.datn.presentation.common.classmanager.ClassManagerViewModel
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.classes.components.ClassList
import com.example.datn.presentation.teacher.classes.components.AddEditClassDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassManagerScreen(
    onNavigateToLessonManager: (classId: String, className: String) -> Unit,
    onNavigateToEnrollmentManagement: ((classId: String, className: String) -> Unit)? = null,
    onNavigateToClassMembers: ((classId: String, className: String) -> Unit)? = null,
    viewModel: ClassManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quản lý lớp học") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ClassManagerEvent.ShowAddClassDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm lớp")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            ClassList(
                classes = state.classes,
                onEdit = { classObj -> viewModel.onEvent(ClassManagerEvent.EditClass(classObj)) },
                onDelete = { classObj -> viewModel.onEvent(ClassManagerEvent.DeleteClass(classObj)) },
                onClick = { selectedClass ->
                    onNavigateToLessonManager(selectedClass.id, selectedClass.name)
                },
                onManageEnrollment = onNavigateToEnrollmentManagement?.let { callback ->
                    { classObj -> callback(classObj.id, classObj.name) }
                },
                onViewMembers = onNavigateToClassMembers?.let { callback ->
                    { classObj -> callback(classObj.id, classObj.name) }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (state.showAddEditDialog) {
            AddEditClassDialog(
                classObj = state.editingClass,
                onDismiss = { viewModel.onEvent(ClassManagerEvent.DismissDialog) },
                onConfirmAdd = { name, code, gradeLevel, subject ->
                    viewModel.onEvent(
                        ClassManagerEvent.ConfirmAddClass(name, code, gradeLevel, subject)
                    )
                },
                onConfirmEdit = { id, name, code, gradeLevel, subject ->
                    viewModel.onEvent(
                        ClassManagerEvent.ConfirmEditClass(id, name, code, gradeLevel, subject)
                    )
                }
            )
        }
        ConfirmationDialog(
            state = state.confirmDeleteState,
            confirmText = "Xóa", // Thay đổi nút xác nhận thành "Xóa"
            onDismiss = { viewModel.dismissConfirmDeleteDialog() },
            // Lấy đối tượng Class từ state.data và truyền vào hàm confirmDeleteClass
            onConfirm = { classObj -> viewModel.confirmDeleteClass(classObj) }
        )
    }
}
