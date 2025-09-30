package com.example.datn.presentation.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AppButton(onClick: () -> Unit, text: String) {
    Button(onClick = onClick) { Text(text) }
}