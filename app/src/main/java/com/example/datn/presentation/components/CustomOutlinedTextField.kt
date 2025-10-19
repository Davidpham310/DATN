package com.example.datn.presentation.components


import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onAction: KeyboardActions = KeyboardActions.Default,
    maxLines: Int = 1,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(text = it) } },
        placeholder = placeholder?.let { { Text(text = it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(text = it) } },
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = onAction,
        visualTransformation = visualTransformation,
        maxLines = maxLines,
        singleLine = singleLine
    )
}