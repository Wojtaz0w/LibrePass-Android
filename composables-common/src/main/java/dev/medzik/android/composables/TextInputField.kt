package dev.medzik.android.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun TextInputField(
    label: String,
    hidden: Boolean = false,
    value: String?,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val hiddenState = remember { mutableStateOf(hidden) }

    OutlinedTextField(
        value = value ?: "",
        onValueChange = onValueChange,
        label = { Text(label) },
        maxLines = 1,
        singleLine = true,
        visualTransformation = (
            if (hidden && hiddenState.value)
                PasswordVisualTransformation()
            else
                VisualTransformation.None
            ),
        trailingIcon = {
            if (hidden) {
                IconButton(onClick = { hiddenState.value = !hiddenState.value }) {
                    Icon(
                        imageVector = (
                            if (hiddenState.value)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff
                            ),
                        contentDescription = null
                    )
                }
            }
        },
        supportingText = {
            if (isError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(text = "")
            }
        },
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TextInputField(
    @StringRes label: Int,
    hidden: Boolean = false,
    value: String?,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextInputField(
        label = stringResource(label),
        hidden = hidden,
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        errorMessage = errorMessage,
        keyboardType = keyboardType
    )
}

@Composable
fun TextInputFieldBase(
    label: String,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    value: String?,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    trailingIcon: @Composable () -> Unit = {}
) {
    val hiddenState = remember { mutableStateOf(hidden) }

    OutlinedTextField(
        value = value ?: "",
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = (
            if (hidden && hiddenState.value)
                PasswordVisualTransformation()
            else
                VisualTransformation.None
            ),
        trailingIcon = {
            Row {
                if (hidden) {
                    IconButton(onClick = { hiddenState.value = !hiddenState.value }) {
                        Icon(
                            imageVector = (
                                if (hiddenState.value)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff
                                ),
                            contentDescription = null
                        )
                    }
                }

                trailingIcon()
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = modifier
    )
}

@Composable
fun TextInputFieldBase(
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    value: String?,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    trailingIcon: @Composable () -> Unit = {}
) {
    TextInputFieldBase(
        label = stringResource(label),
        modifier = modifier,
        hidden = hidden,
        value = value,
        onValueChange = onValueChange,
        keyboardType = keyboardType,
        singleLine = singleLine,
        trailingIcon = trailingIcon
    )
}
