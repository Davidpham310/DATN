package com.example.datn.presentation.common.profile

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var showAvatarDialog by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // File Picker Launcher for Avatar
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(it)
                    val fileName = contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex("_display_name")
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "avatar"
                    val fileSize = contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex("_size")
                        cursor.moveToFirst()
                        cursor.getLong(sizeIndex)
                    } ?: 0L

                    if (inputStream != null && fileSize > 0) {
                        selectedFileName = fileName
                        viewModel.onEvent(
                            EditProfileEvent.UploadAvatar(
                                inputStream = inputStream,
                                fileName = fileName,
                                fileSize = fileSize,
                                contentType = "image/*"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AvatarPicker", "Error reading file", e)
                }
            }
        }
    }

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
                // Avatar Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Avatar",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Hiển thị avatar hiện tại hoặc hình ảnh mặc định
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.avatarUrl != null) {
                                AsyncImage(
                                    model = state.avatarUrl,
                                    contentDescription = "Avatar hiện tại",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        // Nếu tải ảnh thất bại, hiển thị icon mặc định
                                    }
                                )
                            } else {
                                // Hiển thị icon mặc định khi không có URL
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar mặc định",
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showAvatarDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cập nhật avatar")
                        }


                    }
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onEvent(EditProfileEvent.UpdateName(it)) },
                    label = { Text("Tên") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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

    // Avatar Picker Dialog
    if (showAvatarDialog) {
        AvatarPickerDialog(
            currentAvatarUrl = state.avatarUrl,
            isUploading = state.isUploadingAvatar,
            uploadProgress = state.avatarUploadProgress,
            selectedFileName = selectedFileName,
            onDismiss = { showAvatarDialog = false },
            onSelectFile = {
                filePickerLauncher.launch("image/*")
            },
            onConfirm = {
                val currentState = state
                val currentUserId = when {
                    currentState.student != null -> currentState.student!!.userId
                    currentState.teacher != null -> currentState.teacher!!.userId
                    currentState.parent != null -> currentState.parent!!.userId
                    else -> null
                }
                currentUserId?.let {
                    viewModel.saveAvatarToProfile(it)
                    showAvatarDialog = false
                }
            }
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

