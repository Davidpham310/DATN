package com.example.datn.presentation.teacher.classes.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.classmanager.ClassManagerEvent
import com.example.datn.presentation.teacher.classes.ClassManagerViewModel
import com.example.datn.presentation.teacher.classes.components.ClassList
import com.example.datn.presentation.teacher.classes.components.AddEditClassDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassManagerScreen(
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
                modifier = Modifier.fillMaxSize()
            )
        }

        if (state.showAddEditDialog) {
            AddEditClassDialog(
                classObj = state.editingClass,
                onDismiss = { viewModel.onEvent(ClassManagerEvent.DismissDialog) },
                onConfirm = { name, code ->
                    if (state.editingClass == null) {
                        viewModel.onEvent(ClassManagerEvent.ConfirmAddClass(name, code))
                    } else {
                        viewModel.onEvent(
                            ClassManagerEvent.EditClass(
                                state.editingClass!!.copy(
                                    name = name,
                                    classCode = code
                                )
                            )
                        )
                    }
                }
            )
        }
    }
}
