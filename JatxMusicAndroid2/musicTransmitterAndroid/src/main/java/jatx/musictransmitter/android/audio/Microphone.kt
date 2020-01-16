package jatx.musictransmitter.android.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import jatx.musiccommons.Frame
import jatx.musiccommons.frameFromMicRawData

object Microphone {
    private var sampleRate = 0
    private var bufferSize = 0
    private var encoding = 0
    private var channelMask = 0
    @Volatile private var data = ByteArray(0)
    @Volatile private var audioRecord: AudioRecord? = null

    @Throws(MicrophoneInitException::class)
    fun start() {
        if (audioRecord == null) audioRecord = findAudioRecord()
        audioRecord?.startRecording() ?: throw MicrophoneInitException()
    }

    fun stop() {
        audioRecord?.stop()
    }

    @Throws(MicrophoneReadException::class)
    fun readFrame(position: Int): Frame? {
        return try {
            val numBytesRead = audioRecord?.read(data, 0, bufferSize) ?: 0
            frameFromMicRawData(data, numBytesRead, position, sampleRate)
        } catch (e: Throwable) {
            throw MicrophoneReadException(e)
        }
    }

    private fun findAudioRecord(): AudioRecord? {
        val sampleRates =
            intArrayOf(48000, 44100, 32000, 22050, 16000, 11025, 8000)
        for (rate in sampleRates) {
            for (audioFormat in shortArrayOf(AudioFormat.ENCODING_PCM_16BIT.toShort())) {
                for (channelConfig in shortArrayOf(AudioFormat.CHANNEL_IN_STEREO.toShort() )) {
                    try {
                        println("Trying mic config: attempting rate $rate Hz, bits: $audioFormat, channel: $channelConfig")
                        bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig.toInt(), audioFormat.toInt())
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) { // check if we can instantiate and have a success
                            data = ByteArray(bufferSize)
                            val audioRecord = AudioRecord(
                                MediaRecorder.AudioSource.DEFAULT,
                                rate,
                                channelConfig.toInt(),
                                audioFormat.toInt(),
                                bufferSize
                            )
                            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                                println("Attempt success")
                                sampleRate = rate
                                encoding = audioFormat.toInt()
                                channelMask = channelConfig.toInt()
                                return audioRecord
                            }
                        }
                    } catch (e: Throwable) {
                        println("Find audio record attempt failed: $rate")
                    }
                }
            }
        }
        return null
    }

}

class MicrophoneReadException: Exception {
    constructor(e: Throwable): super(e)
}

class MicrophoneInitException: Exception()