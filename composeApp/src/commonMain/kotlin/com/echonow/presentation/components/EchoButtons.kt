package com.echonow.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.echonow.theme.LocalButtonCustomization

@Composable
fun EchoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val customization = LocalButtonCustomization.current
    val fill = customization.fillColor ?: MaterialTheme.colorScheme.primary
    val text = customization.textColor ?: MaterialTheme.colorScheme.onPrimary
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape ?: customization.shape,
        colors = colors ?: ButtonDefaults.buttonColors(
            containerColor = fill,
            contentColor = text,
            disabledContainerColor = fill.copy(alpha = .12f),
            disabledContentColor = text.copy(alpha = .38f),
        ),
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun EchoOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val customization = LocalButtonCustomization.current
    val text = customization.textColor ?: MaterialTheme.colorScheme.primary
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape ?: customization.shape,
        colors = colors ?: ButtonDefaults.outlinedButtonColors(contentColor = text),
        border = border ?: BorderStroke(
            1.dp,
            customization.borderColor ?: MaterialTheme.colorScheme.outline,
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun EchoTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val customization = LocalButtonCustomization.current
    val text = customization.textColor ?: MaterialTheme.colorScheme.primary
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape ?: customization.shape,
        colors = colors ?: ButtonDefaults.textButtonColors(contentColor = text),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
