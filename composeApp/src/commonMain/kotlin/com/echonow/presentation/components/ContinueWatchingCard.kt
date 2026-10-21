package com.echonow.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.echonow.domain.model.MediaType
import com.echonow.domain.repository.WatchProgress
import com.echonow.theme.LocalButtonCustomization

@Composable
fun ContinueWatchingCard(
    progress: WatchProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 280.dp,
) {
    var focused by remember(progress.entryId) { mutableStateOf(false) }
    val interactionSource = remember(progress.entryId) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val emphasized = focused || hovered
    val scale by animateFloatAsState(
        targetValue = if (emphasized) 1.045f else 1f,
        animationSpec = tween(180),
        label = "continueWatchingScale",
    )
    val playAlpha by animateFloatAsState(
        targetValue = if (emphasized) 1f else .58f,
        animationSpec = tween(180),
        label = "continueWatchingPlay",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress.watchedPercentage.coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "continueWatchingProgress",
    )
    val buttonStyle = LocalButtonCustomization.current
    val shape: Shape = buttonStyle.shape
    val progressColor = buttonStyle.borderColor?.let { borderColor ->
        val inverted = Color(1f - borderColor.red, 1f - borderColor.green, 1f - borderColor.blue)
        if (inverted.luminance() < .35f) Color.White else inverted
    } ?: Color.White
    val progressTrackColor = Color.Black.copy(alpha = .68f)

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (emphasized) 1f else 0f)
            .clip(shape)
            .background(Color(0xFF242424))
            .border(
                width = if (emphasized) 2.dp else 1.dp,
                color = buttonStyle.borderColor ?: MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (emphasized) 1f else .65f,
                ),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(),
    ) {
        AsyncImage(
            model = progress.posterUrl,
            imageLoader = LocalEchoImageLoader.current,
            contentDescription = "${progress.title} episode thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(112.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = .9f)),
                    ),
                ),
        )
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = if (emphasized) "Resume ${progress.title}" else null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = .48f * playAlpha))
                .padding(8.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        ) {
            Text(
                progress.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val episodeContext = buildString {
                if (progress.type == MediaType.MOVIE) {
                    append("Continue movie")
                } else if (progress.season > 0 && progress.episode > 0) {
                    append("S${progress.season}:E${progress.episode}")
                    append(" - ")
                    append(progress.episodeTitle.ifBlank { "Continue episode" })
                } else {
                    append(progress.episodeTitle.ifBlank { "Continue episode" })
                }
            }
            Text(
                episodeContext,
                color = Color.White.copy(alpha = .86f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(progressTrackColor),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxSize()
                    .shadow(
                        elevation = if (emphasized) 7.dp else 3.dp,
                        shape = CircleShape,
                        ambientColor = progressColor.copy(alpha = .55f),
                        spotColor = progressColor.copy(alpha = .55f),
                    )
                    .clip(CircleShape)
                    .background(progressColor),
            )
        }
    }
}
