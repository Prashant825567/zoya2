package com.zoya.ai.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class AudioPlayer(
    private val onPlayStateChange: (Boolean) -> Unit
) {
    companion object {
        private const val TAG = "AudioPlayer"
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    @Volatile
    private var isPlaying = false
    private var playbackThread: Thread? = null

    fun initialize() {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        startPlaybackThread()
    }

    private fun startPlaybackThread() {
        isPlaying = true
        playbackThread = thread(isDaemon = true) {
            try {
                while (isPlaying) {
                    val chunk = audioQueue.poll()
                    if (chunk != null) {
                        onPlayStateChange(true)
                        try {
                            audioTrack?.write(chunk, 0, chunk.size)
                        } catch (e: Exception) {
                            Log.e(TAG, "Write error: ${e.message}")
                        }
                    } else {
                        onPlayStateChange(false)
                        try {
                            Thread.sleep(10)
                        } catch (_: InterruptedException) {
                            // Thread interrupted during shutdown — exit gracefully
                            break
                        }
                    }
                }
            } catch (_: InterruptedException) {
                // Graceful exit
            } catch (e: Exception) {
                Log.e(TAG, "Playback thread error: ${e.message}")
            }
            Log.d(TAG, "Playback thread stopped")
        }
    }

    suspend fun addAudioChunk(base64Data: String) = withContext(Dispatchers.IO) {
        try {
            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            audioQueue.add(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Decode error: ${e.message}")
        }
    }

    fun stop() {
        audioQueue.clear()
        onPlayStateChange(false)
    }

    fun release() {
        isPlaying = false
        playbackThread?.interrupt()
        playbackThread = null
        audioQueue.clear()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Release error: ${e.message}")
        }
        audioTrack = null
    }
}
