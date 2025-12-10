package com.example.datn.presentation.common.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.UserRole
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userId: String,
    role: String,
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(userId, role) {
        viewModel.onEvent(EditProfileEvent.LoadProfile(userId, role))
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    val screenTitle = when (role.uppercase()) {
        UserRole.STUDENT.name -> "Chỉnh sửa hồ sơ học sinh"
        UserRole.TEACHER.name -> "Chỉnh sửa hồ sơ giáo viên"
        UserRole.PARENT.name -> "Chỉnh sửa hồ sơ phụ huynh"
        else -> "Chỉnh sửa hồ sơ"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Render role-specific fields
                when (role.uppercase()) {
                    UserRole.STUDENT.name -> StudentProfileFields(state, viewModel, { showDatePicker = true })
                    UserRole.TEACHER.name -> TeacherProfileFields(state, viewModel)
                    UserRole.PARENT.name -> ParentProfileFields(state)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = { viewModel.onEvent(EditProfileEvent.SaveProfile) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !state.isLoading
                ) {
                    Text("Lưu thay đổi")
                }

                // Error Message
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                viewModel.onEvent(EditProfileEvent.UpdateDateOfBirth(date))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun StudentProfileFields(
    state: EditProfileState,
    viewModel: EditProfileViewModel,
    onDatePickerClick: () -> Unit
) {
    // Grade Level Field
    OutlinedTextField(
        value = state.gradeLevel,
        onValueChange = { viewModel.onEvent(EditProfileEvent.UpdateGradeLevel(it)) },
        label = { Text("Lớp học") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Date of Birth Field
    OutlinedTextField(
        value = state.dateOfBirth?.toString() ?: "",
        onValueChange = {},
        label = { Text("Ngày sinh") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onDatePickerClick) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        }
    )
}

@Composable
private fun TeacherProfileFields(
    state: EditProfileState,
    viewModel: EditProfileViewModel
) {
    // Specialization Field
    OutlinedTextField(
        value = state.specialization,
        onValueChange = { viewModel.onEvent(EditProfileEvent.UpdateSpecialization(it)) },
        label = { Text("Chuyên ngành") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Level Field
    OutlinedTextField(
        value = state.level,
        onValueChange = { viewModel.onEvent(EditProfileEvent.UpdateLevel(it)) },
        label = { Text("Trình độ") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Experience Years Field
    OutlinedTextField(
        value = state.experienceYears,
        onValueChange = { viewModel.onEvent(EditProfileEvent.UpdateExperienceYears(it)) },
        label = { Text("Năm kinh nghiệm") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@Composable
private fun ParentProfileFields(state: EditProfileState) {
    // Profile Info Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Thông tin phụ huynh",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "ID: ${state.parent?.id ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Ngày tạo: ${state.parent?.createdAt ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Cập nhật lần cuối: ${state.parent?.updatedAt ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn ngày sinh") },
        text = {
            Column {
                Text("Ngày: ${selectedDate.dayOfMonth}")
                Slider(
                    value = selectedDate.dayOfMonth.toFloat(),
                    onValueChange = {
                        try {
                            selectedDate = selectedDate.withDayOfMonth(it.toInt())
                        } catch (e: Exception) {
                            // Handle invalid day
                        }
                    },
                    valueRange = 1f..31f,
                    steps = 30
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Tháng: ${selectedDate.monthValue}")
                Slider(
                    value = selectedDate.monthValue.toFloat(),
                    onValueChange = {
                        selectedDate = selectedDate.withMonth(it.toInt())
                    },
                    valueRange = 1f..12f,
                    steps = 11
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Năm: ${selectedDate.year}")
                Slider(
                    value = selectedDate.year.toFloat(),
                    onValueChange = {
                        selectedDate = selectedDate.withYear(it.toInt())
                    },
                    valueRange = 1990f..2024f,
                    steps = 33
                )
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDate) }) {
                Text("Chọn")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

