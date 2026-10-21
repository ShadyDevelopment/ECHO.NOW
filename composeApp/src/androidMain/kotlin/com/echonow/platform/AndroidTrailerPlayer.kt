package com.echonow.platform

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
actual fun PlatformTrailerPlayer(videoId: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dialogHostView = LocalView.current
    var fullscreen by remember(videoId) { mutableStateOf(false) }
    var exitFullscreen by remember(videoId) { mutableStateOf<(() -> Unit)?>(null) }
    var playbackError by remember(videoId) { mutableStateOf<String?>(null) }
    val playerViewReference = remember(videoId) { mutableStateOf<YouTubePlayerView?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        DialogBackdropEffect()
        DisposableEffect(fullscreen, dialogHostView) {
            val window = (dialogHostView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                window?.let { WindowCompat.getInsetsController(it, it.decorView).hide(WindowInsetsCompat.Type.systemBars()) }
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(Color(0xF20A0A0E)),
            contentAlignment = Alignment.Center,
        ) {
            val compact = maxWidth < 600.dp && maxHeight > maxWidth
            val playerModifier = if (fullscreen) {
                Modifier.fillMaxSize()
            } else {
                val playerWidth = if (compact) {
                    minOf(maxWidth * .96f, maxHeight * .5f * (16f / 9f), 1120.dp)
                } else {
                    minOf(maxWidth * .92f, maxHeight * .78f * (16f / 9f), 1120.dp)
                }
                Modifier.width(playerWidth).aspectRatio(16f / 9f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            }
            Box(playerModifier.background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(
                    modifier = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                    factory = { viewContext ->
                        val container = FrameLayout(viewContext)
                        val playerView = YouTubePlayerView(viewContext)
                        playerViewReference.value = playerView
                        val matchParent = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        container.addView(playerView, matchParent)
                        lifecycle.addObserver(playerView)
                        playerView.addFullscreenListener(
                            object : FullscreenListener {
                                override fun onEnterFullscreen(
                                    fullscreenView: View,
                                    exitFullscreenCallback: () -> Unit,
                                ) {
                                    exitFullscreen = exitFullscreenCallback
                                    container.removeView(playerView)
                                    container.removeAllViews()
                                    container.addView(fullscreenView, matchParent)
                                    fullscreen = true
                                }

                                override fun onExitFullscreen() {
                                    container.removeAllViews()
                                    container.addView(playerView, matchParent)
                                    exitFullscreen = null
                                    fullscreen = false
                                }
                            },
                        )
                        playerView.addYouTubePlayerListener(
                            object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.loadVideo(videoId, 0f)
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError,
                                ) {
                                    playbackError = "This trailer could not be played ($error)."
                                }
                            },
                        )
                        container
                    },
                    onRelease = { container ->
                        playerViewReference.value?.let { player ->
                            lifecycle.removeObserver(player)
                            player.release()
                        }
                        playerViewReference.value = null
                        container.removeAllViews()
                    },
                )
            }
            IconButton(
                onClick = {
                    if (fullscreen) exitFullscreen?.invoke() else onClose()
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = if (fullscreen) "Exit trailer fullscreen" else "Close trailer",
                    tint = Color.White,
                )
            }
            if (!fullscreen) {
                Text(
                    "Trailer",
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
            playbackError?.let { message ->
                Text(
                    message,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(20.dp)
                        .background(Color.Black.copy(alpha = .82f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                )
            }
        }
    }
}
