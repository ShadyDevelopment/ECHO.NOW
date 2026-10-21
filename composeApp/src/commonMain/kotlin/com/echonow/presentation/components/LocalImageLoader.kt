package com.echonow.presentation.components

import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader

val LocalEchoImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("ECHO NOW image loader has not been initialized.")
}
