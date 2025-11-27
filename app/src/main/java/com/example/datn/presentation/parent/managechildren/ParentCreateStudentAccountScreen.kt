package com.example.datn.presentation.parent.managechildren

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.RelationshipType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentCreateStudentAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: ParentCreateStudentAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var dateOfBirthText by remember { mutableStateOf("") }
    var selectedRelationship by remember { mutableStateOf(RelationshipType.GUARDIAN) }
    var isPrimaryGuardian by remember { mutableStateOf(true) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
            viewModel.onEvent(ParentCreateStudentAccountEvent.ClearMessages)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo tài khoản học sinh") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Họ và tên học sinh") },
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email đăng nhập của học sinh") },
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mật khẩu (do phụ huynh đặt)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = gradeLevel,
                onValueChange = { gradeLevel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lớp/Khối (ví dụ: 5A, 6B)") },
                singleLine = true
            )

            OutlinedTextField(
                value = dateOfBirthText,
                onValueChange = { dateOfBirthText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ngày sinh (yyyy-MM-dd)") },
                singleLine = true
            )

            Text(
                text = "Mối quan hệ với học sinh",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            RelationshipType.values().forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedRelationship == type,
                        onClick = { selectedRelationship = type }
                    )
                    Text(type.displayName)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isPrimaryGuardian,
                    onCheckedChange = { isPrimaryGuardian = it }
                )
                Text("Phụ huynh này là người giám hộ chính")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.onEvent(
                        ParentCreateStudentAccountEvent.Submit(
                            name = name,
                            email = email,
                            password = password,
                            gradeLevel = gradeLevel,
                            dateOfBirthText = dateOfBirthText,
                            relationship = selectedRelationship,
                            isPrimaryGuardian = isPrimaryGuardian
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Tạo tài khoản học sinh")
            }
        }
    }
}
