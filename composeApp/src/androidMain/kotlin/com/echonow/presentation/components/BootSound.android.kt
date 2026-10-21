package com.echonow.presentation.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

@Composable
actual fun BootSound() {
    val context = LocalContext.current

    DisposableEffect(context) {
        val stages = listOf("boot-stage-1.wav", "boot-stage-2.wav", "boot-stage-3.wav")
        var nextStage = 0
        var disposed = false
        var activePlayer: MediaPlayer? = null
        lateinit var playNext: () -> Unit

        playNext = {
            if (!disposed && nextStage < stages.size) {
                val stage = stages[nextStage++]
                val player = MediaPlayer()
                try {
                    context.assets.openFd(stage).use { asset ->
                        player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
                    }
                    player.setOnCompletionListener {
                        it.release()
                        if (activePlayer === it) activePlayer = null
                        playNext()
                    }
                    player.setOnErrorListener { failedPlayer, what, extra ->
                        Log.e("BootSound", "Boot sound playback failed: what=$what extra=$extra")
                        failedPlayer.release()
                        if (activePlayer === failedPlayer) activePlayer = null
                        playNext()
                        true
                    }
                    player.prepare()
                    activePlayer = player
                    player.start()
                } catch (exception: IOException) {
                    Log.e("BootSound", "Unable to load boot sound stage $stage", exception)
                    player.release()
                    playNext()
                }
            }
        }

        playNext()
        onDispose {
            disposed = true
            activePlayer?.release()
            activePlayer = null
        }
    }
}
