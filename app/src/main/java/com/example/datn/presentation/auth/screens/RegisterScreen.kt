package com.example.datn.presentation.auth.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.UserRole
import com.example.datn.presentation.auth.AuthViewModel
import com.example.datn.presentation.common.auth.AuthEvent
import com.example.datn.presentation.components.AuthPasswordField
import com.example.datn.presentation.components.AuthTextField
import com.example.datn.presentation.components.RoleSelector
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateLogin: () -> Unit
) {
    val state = viewModel.state.collectAsState().value
    var selectedRole by remember { mutableStateOf(UserRole.PARENT) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            delay(2000)
            onNavigateLogin()
            viewModel.clearNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Đăng ký", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        RoleSelector(
            roles = listOf(UserRole.TEACHER, UserRole.PARENT),
            selectedRole = selectedRole,
            onRoleSelected = { selectedRole = it }
        )

        Spacer(Modifier.height(16.dp))

        AuthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Họ và tên"
        )

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email"
        )

        AuthPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Mật khẩu"
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.onEvent((AuthEvent.OnRegister(email, password, name, selectedRole)))
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Đăng ký")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onNavigateLogin) {
            Text("Đã có tài khoản? Đăng nhập")
        }
    }
}

