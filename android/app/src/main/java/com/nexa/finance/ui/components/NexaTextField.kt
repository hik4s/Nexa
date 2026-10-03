package com.nexa.finance.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nexa.finance.ui.theme.NexaRadii

enum class NexaTextFieldVariant {
    Outlined,
    Filled,
}

/**
 * NEXA text-field primitive.
 *
 * The wrapper centralizes geometry and semantic states while preserving
 * Material 3 interaction behavior such as focus, press and accessibility.
 */
@Composable
fun NexaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    variant: NexaTextFieldVariant = NexaTextFieldVariant.Outlined,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = RoundedCornerShape(NexaRadii.md),
) {
    val labelContent: (@Composable (() -> Unit))? = label?.let { text ->
        { Text(text) }
    }
    val placeholderContent: (@Composable (() -> Unit))? = placeholder?.let { text ->
        { Text(text) }
    }
    val supportingContent: (@Composable (() -> Unit))? = supportingText?.let { text ->
        { Text(text) }
    }

    when (variant) {
        NexaTextFieldVariant.Outlined -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            label = labelContent,
            placeholder = placeholderContent,
            supportingText = supportingContent,
            isError = isError,
            singleLine = singleLine,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = shape,
            colors = NexaOutlinedTextFieldColors(),
        )

        NexaTextFieldVariant.Filled -> TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            label = labelContent,
            placeholder = placeholderContent,
            supportingText = supportingContent,
            isError = isError,
            singleLine = singleLine,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = shape,
            colors = NexaFilledTextFieldColors(),
        )
    }
}

@Composable
private fun NexaOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,
    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
)

@Composable
private fun NexaFilledTextFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,
)
