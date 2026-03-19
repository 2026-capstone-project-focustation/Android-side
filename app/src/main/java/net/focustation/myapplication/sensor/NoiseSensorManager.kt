package net.focustation.myapplication.sensor

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class NoiseSensorManager {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // RECORD_AUDIO 권한이 이미 확인된 후에 호출해야 한다
    @SuppressLint("MissingPermission")
    fun getNoiseFlow(): Flow<Double> = callbackFlow {
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
        )

        val buffer = ShortArray(bufferSize)
        audioRecord.startRecording()

        val job = launch(Dispatchers.IO) {
            while (isActive && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    var sum = 0.0
                    for (i in 0 until readSize) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = sqrt(sum / readSize)
                    val db = if (rms > 0) 20 * log10(rms / 32767.0) + 90 else 0.0
                    trySend(db)
                }
            }
        }

        awaitClose {
            job.cancel()
            audioRecord.stop()
            audioRecord.release()
        }
    }
}
