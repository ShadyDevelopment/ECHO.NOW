package com.echonow.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException
import java.io.IOException

@Composable
actual fun BootSound() {
    DisposableEffect(Unit) {
        val playbackThread = Thread({
            val stages = listOf("boot-stage-1.wav", "boot-stage-2.wav", "boot-stage-3.wav")
            for (stage in stages) {
                if (Thread.currentThread().isInterrupted) break
                val audio = Thread.currentThread().contextClassLoader.getResourceAsStream(stage)
                if (audio == null) {
                    System.err.println("BootSound: missing audio resource $stage")
                    continue
                }
                try {
                    audio.use { source ->
                        AudioSystem.getAudioInputStream(source).use { stream ->
                            AudioSystem.getClip().use { clip ->
                                clip.open(stream)
                                clip.start()
                                while (clip.isRunning && !Thread.currentThread().isInterrupted) {
                                    Thread.sleep(20)
                                }
                            }
                        }
                    }
                } catch (exception: IOException) {
                    System.err.println("BootSound: unable to read $stage: ${exception.message}")
                } catch (exception: UnsupportedAudioFileException) {
                    System.err.println("BootSound: unsupported audio file $stage: ${exception.message}")
                } catch (exception: LineUnavailableException) {
                    System.err.println("BootSound: audio output unavailable: ${exception.message}")
                    break
                } catch (exception: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }, "boot-sound").apply {
            isDaemon = true
            start()
        }

        onDispose { playbackThread.interrupt() }
    }
}
