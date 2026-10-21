package com.echonow.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun BootLogo(modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(context) {
        context.assets.open("echo_now_logo_with_text.png").use(BitmapFactory::decodeStream).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = "ECHO NOW",
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}
