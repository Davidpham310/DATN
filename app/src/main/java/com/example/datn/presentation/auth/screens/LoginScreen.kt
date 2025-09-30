package com.example.datn.presentation.auth.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.core.presentation.notifications.NotificationHost
import com.example.datn.presentation.auth.AuthViewModel
import com.example.datn.presentation.common.AuthEvent

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    Column {
        // Loading indicator
        if (state.isLoading) { CircularProgressIndicator() }

        // Error message
        state.error?.let {
            Text("Error: $it")
            Log.d("LoginScreen", "Error: $it")
        }

        // Email input
        TextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") }
        )

        // Password input
        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") }
        )

        // Login button
        Button(
            onClick = {
                viewModel.onEvent(AuthEvent.OnLogin(email.value, password.value))
            }
        ) {
            Text("Login")
        }

        // Notification host (gắn vào NotificationManager của ViewModel)
        NotificationHost(viewModel.notificationManager)
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
