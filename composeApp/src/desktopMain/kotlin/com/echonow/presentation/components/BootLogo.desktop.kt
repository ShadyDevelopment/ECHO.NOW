package com.echonow.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import echo_now.composeapp.generated.resources.Res
import echo_now.composeapp.generated.resources.echo_now_logo_with_text
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun BootLogo(modifier: Modifier) {
    Image(
        painter = painterResource(Res.drawable.echo_now_logo_with_text),
        contentDescription = "ECHO NOW",
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}
