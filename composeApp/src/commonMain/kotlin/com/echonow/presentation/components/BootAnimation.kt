package com.echonow.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun BootAnimation(onFinished: () -> Unit) {
    BootSound()
    val expansion = remember { Animatable(0f) }
    val opacity = remember { Animatable(1f) }
    val ambient = rememberInfiniteTransition()
    val glow by ambient.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
    )
    val drift by ambient.animateFloat(
        initialValue = -24f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(5_000), RepeatMode.Reverse),
    )
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1_000)
        expansion.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1_200,
                easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
            ),
        )
        delay(600)
        opacity.animateTo(0f, animationSpec = tween(durationMillis = 700))
        finished = true
        onFinished()
    }

    if (finished) return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacity.value }
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF171025),
                        Color(0xFF0D0D16),
                        Color(0xFF030307),
                    ),
                ),
            )
            .zIndex(1f),
        contentAlignment = Alignment.Center,
    ) {
        val logoAspectRatio = 1744f / 592f
        val targetWidth = minOf(maxWidth * 0.88f, maxHeight * 0.52f * logoAspectRatio, 680.dp)
        val logoHeight = targetWidth / logoAspectRatio
        Box(
            Modifier
                .width(maxOf(maxWidth, maxHeight) * 0.72f)
                .aspectRatio(1f)
                .offset(x = drift.dp, y = (-48f).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF8B45FF).copy(alpha = glow), Color(0xFF42216F).copy(alpha = glow * .45f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .width(maxOf(maxWidth, maxHeight) * 0.54f)
                .aspectRatio(1f)
                .offset(x = (-drift * 1.3f).dp, y = 110.dp)
                .blur(110.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF19B9D8).copy(alpha = glow * .24f), Color.Transparent),
                    ),
                ),
        )
        val initialWidth = targetWidth * (115f / 360f)
        val containerWidth = initialWidth + (targetWidth - initialWidth) * expansion.value
        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .width(containerWidth + 12.dp)
                .height(logoHeight + 12.dp)
                .shadow(42.dp, RoundedCornerShape(22.dp), ambientColor = Color(0xFF9C5CFF).copy(alpha = glow), spotColor = Color(0xFF22C7E8).copy(alpha = glow * .55f))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = .18f),
                            Color(0xFF8B5CF6).copy(alpha = .72f),
                            Color(0xFF38BDF8).copy(alpha = .48f),
                            Color.White.copy(alpha = .12f),
                        ),
                    ),
                )
                .padding(1.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(Color(0xFF08080D).copy(alpha = .94f))
                .padding(5.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1D1928), Color(0xFF09090E), Color.Black),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = .16f), shape),
            contentAlignment = Alignment.CenterStart,
        ) {
            BootLogo(
                modifier = Modifier
                    .width(targetWidth)
                    .aspectRatio(logoAspectRatio),
            )
        }
    }
}
