package com.example.datn.presentation.auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.UserRole
import com.example.datn.presentation.auth.event.AuthEvent
import com.example.datn.presentation.auth.viewmodel.AuthViewModel
import com.example.datn.presentation.components.AuthPasswordField
import com.example.datn.presentation.components.AuthTextField
import com.example.datn.presentation.components.RoleSelector

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateRegister: () -> Unit = {},
    onNavigateForgotPassword: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val state = viewModel.state.collectAsState().value
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            onNavigate(route)
            viewModel.clearNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Loading indicator
        if (state.isLoading) {
            CircularProgressIndicator()
        }

//        // Error message
//        state.error?.let {
//            Text("Error: $it", color = Color.Red)
//            Log.d("LoginScreen", "Error: $it")
//        }

        Text(
            text = "Đăng nhập",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chọn vai trò
        RoleSelector(
            roles = listOf(UserRole.TEACHER, UserRole.PARENT , UserRole.STUDENT),
            selectedRole = selectedRole,
            onRoleSelected = { selectedRole = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email input
        AuthTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = "Email"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password input
        AuthPasswordField(
            value = password.value,
            onValueChange = { password.value = it },
            label = "Mật khẩu"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = {
                viewModel.onEvent(AuthEvent.OnLogin(email.value, password.value , selectedRole ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng nhập")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot password
        TextButton(onClick = onNavigateForgotPassword) {
            Text("Quên mật khẩu?")
        }

        // Register
        TextButton(onClick = onNavigateRegister) {
            Text("Chưa có tài khoản? Đăng ký")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
