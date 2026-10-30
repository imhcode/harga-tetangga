package com.imhcode.hargatetangga.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

// F4-03: records a short spoken list as 24 kHz mono 16-bit PCM, the format Muse Voice Transcribe accepts,
// and wraps it in a WAV header. Stops by itself after MAX_SECONDS.
class VoiceRecorder {
    private var record: AudioRecord? = null
    private var job: Job? = null
    private val pcm = ByteArrayOutputStream()

    val seconds: Int get() = pcm.size() / BYTES_PER_SECOND

    // Caller must hold RECORD_AUDIO; the screen asks for it before calling start().
    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        pcm.reset()
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, 8192),
        )
        record = recorder
        recorder.startRecording()
        job = scope.launch(Dispatchers.IO) {
            val chunk = ByteArray(4096)
            while (isActive && pcm.size() < MAX_SECONDS * BYTES_PER_SECOND) {
                val n = recorder.read(chunk, 0, chunk.size)
                if (n > 0) synchronized(pcm) { pcm.write(chunk, 0, n) }
            }
        }
    }

    suspend fun stop(): ByteArray {
        job?.cancelAndJoin()
        record?.run {
            stop()
            release()
        }
        record = null
        return wav(synchronized(pcm) { pcm.toByteArray() })
    }

    companion object {
        const val SAMPLE_RATE = 24_000
        const val MAX_SECONDS = 60
        private const val BYTES_PER_SECOND = SAMPLE_RATE * 2

        fun wav(pcm: ByteArray): ByteArray {
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1) // PCM, mono
                putInt(SAMPLE_RATE); putInt(BYTES_PER_SECOND); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(pcm.size)
            }
            return header.array() + pcm
        }
    }
}
