package com.example.datn.presentation.auth.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.UserRole
import com.example.datn.presentation.auth.AuthViewModel
import com.example.datn.presentation.common.auth.AuthEvent
import com.example.datn.presentation.components.AuthTextField
import com.example.datn.presentation.components.RoleSelector

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateLogin: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.TEACHER) }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Quên mật khẩu", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        RoleSelector(
            roles = listOf(UserRole.TEACHER, UserRole.PARENT, UserRole.STUDENT),
            selectedRole = selectedRole,
            onRoleSelected = { selectedRole = it }
        )

        Spacer(Modifier.height(16.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email"
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.onEvent(AuthEvent.OnForgotPassword(email, selectedRole))
            }
        )
        {
            Text("Quên mật khẩu")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onNavigateLogin) {
            Text("Quay lại đăng nhập")
        }
    }
}
