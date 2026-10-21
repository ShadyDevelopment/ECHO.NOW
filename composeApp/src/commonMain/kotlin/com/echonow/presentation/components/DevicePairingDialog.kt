package com.echonow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.echonow.presentation.components.EchoTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echonow.platform.pairDeviceWithCode
import com.echonow.platform.DialogBackdropEffect
import kotlinx.coroutines.launch

@Composable
fun DevicePairingDialog(
    onDismiss: () -> Unit,
    onPaired: () -> Unit = {},
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }
    var resetFocus by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(resetFocus) { focusRequester.requestFocus() }
    LaunchedEffect(code) {
        if (code.length == 6 && !pairing) {
            pairing = true
            error = null
            val result = runCatching { pairDeviceWithCode(code) }
                .getOrElse { it.message ?: "Pairing failed. Make sure both devices are online on the same Wi-Fi network." }
            pairing = false
            if (result == null) {
                onPaired()
                onDismiss()
            } else {
                code = ""
                error = result
                resetFocus++
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15151C),
        title = { Text("ECHO Pair") },
        text = {
            DialogBackdropEffect()
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enter remote device one-time pairing code")
                BasicTextField(
                    value = code,
                    onValueChange = { input ->
                        if (!pairing) {
                            code = input.filter(Char::isDigit).take(6)
                            if (error != null) error = null
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                    cursorBrush = SolidColor(Color.Transparent),
                    decorationBox = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(6) { index ->
                                val selected = index == code.length && !pairing
                                Box(
                                    modifier = Modifier.size(width = 40.dp, height = 48.dp)
                                        .border(
                                            width = if (error != null) 2.dp else if (selected) 2.dp else 1.dp,
                                            color = when {
                                                error != null -> MaterialTheme.colorScheme.error
                                                selected -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.outline
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .clickable { focusRequester.requestFocus() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(code.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    },
                )
                if (pairing) Text("Pairing…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !pairing) { Text("Cancel") }
        },
    )
}
