package jatx.musictransmitter.android.threads

import jatx.debug.logError
import jatx.musiccommons.WrongFrameException
import jatx.musiccommons.frameToByteArray
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.audio.*
import jatx.musictransmitter.android.data.MIC_PATH
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.*

const val CONNECT_PORT_PLAYER = 7171

class TransmitterPlayer(
    @Volatile private var uiController: UIController,
    @Volatile private var decoder: Mp3Decoder
): Thread() {

    @Volatile
    var forceDisconnectFlag = false

    @Volatile var microphoneOk = false

    @Volatile
    var count = 0
    @Volatile
    var isPlaying = false

    @Volatile
    var path: String? = null

    @Volatile
    var files: List<File> = listOf()
        set(value) {
            field = value
            count = value.size
        }

    @Volatile
    var position = 0
        set(value) {
            pause()

            if (value < 0 || count <= 0) return

            field = value
            if (position >= count) position = 0

            println("(player) position: $position")

            path = files[position].absolutePath
            println("(player) path: $path")

            try {
                decoder.setPath(path!!)
                decoder.position = position
            } catch (e: Mp3DecoderException) {
                logError(e)
            }

            play()
        }

    @Volatile
    var t1: Long = 0
    @Volatile
    var t2: Long = 0
    @Volatile
    var dt = 0f

    var tc: TransmitterController? = null

    private var ss: ServerSocket? = null
    private var os: OutputStream? = null

    override fun run() {
        try {
            while (true) {
                sleep(100)
                ss = ServerSocket(CONNECT_PORT_PLAYER)
                println("(player) new server socket")
                try {
                    ss?.soTimeout = SO_TIMEOUT
                    val s = ss?.accept()
                    os = s?.getOutputStream()
                    println("(player) socket connect")
                    uiController.setWifiStatus(true)
                    translateMusic()
                } catch (e: SocketTimeoutException) {
                    println("(player) socket timeout")
                } catch (e: ForceDisconnectException) {
                    println("(player) socket force disconnect")
                } catch (e: IOException) {
                    println("(player) socket disconnect")
                    tc?.forceDisconnectFlag = true
                    sleep(250)
                } finally {
                    forceDisconnectFlag = false
                    decoder.disconnectResetTimeFlag = true
                    os?.close()
                    println("(player) outstream closed")
                    ss?.close()
                    println("(player) server socket closed")
                    uiController.setWifiStatus(false)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            println("(player) thread interrupted")
            os?.close()
            println("(player) outstream closed")
            ss?.close()
            println("(player) server socket closed")
        } finally {
            println("(player) thread finished")
        }
    }

    fun play() {
        isPlaying = true
        if (path == MIC_PATH) {
            try {
                Microphone.start()
                microphoneOk = true
            } catch (e: MicrophoneInitException) {
                logError(e)
                microphoneOk = false
                uiController.errorMsg(R.string.toast_cannot_init_microphone)
            }
        }
    }

    fun pause() {
        isPlaying = false
        if (path == MIC_PATH) {
            Microphone.stop()
            microphoneOk = false
        }
    }

    fun seek(progress: Double) {
        val needToPlay = isPlaying
        pause()
        try {
            decoder.seek(progress)
        } catch (e: Mp3DecoderException) {
            logError(e)
        }
        if (needToPlay) play()
    }

    private fun translateMusic() {
        var data: ByteArray? = null

        t1 = System.currentTimeMillis()
        t2 = t1
        dt = 0f

        while (true) {
            if (isPlaying) {
                if (decoder.resetTimeFlag) {
                    do {
                        t2 = System.currentTimeMillis()
                        dt = decoder.msTotal - (t2 - t1)
                        sleep(10)
                    } while (dt > 0)
                    decoder.msRead = 0f
                    decoder.msTotal = 0f
                    t1 = System.currentTimeMillis()
                    t2 = t1
                    decoder.resetTimeFlag = false
                }
                if (decoder.disconnectResetTimeFlag) {
                    t1 = Date().time
                    t2 = t1
                    decoder.msRead = 0f
                    decoder.msTotal = 0f
                    decoder.disconnectResetTimeFlag = false
                }
                try {
                    data = if (path == MIC_PATH) {
                        if (microphoneOk)
                            frameToByteArray(Microphone.readFrame(position))
                        else
                            null
                    } else {
                        frameToByteArray(decoder.readFrame())
                    }
                } catch (e: Mp3DecoderException) {
                    println("(player) decoder exception")
                    data = null
                    sleep(200)
                } catch (e: MicrophoneReadException) {
                    println("(player) microphone read exception")
                    data = null
                    sleep(200)
                } catch (e: TrackFinishException) {
                    println("(player) track finish")
                    nextTrack()
                    data = null
                    sleep(200)
                } catch (e: WrongFrameException) {
                    println("(player) wrong frame")
                    nextTrack()
                    data = null
                    sleep(200)
                }
                if (data != null) {
                    os!!.write(data)
                    os!!.flush()
                }
            } else {
                sleep(10)
                decoder.msRead = 0f
                decoder.msTotal = 0f
                t1 = Date().time
                t2 = t1
                dt = 0f
            }
            if (forceDisconnectFlag) {
                println("(player) disconnect flag: throwing DisconnectException")
                throw ForceDisconnectException()
            }
            if (decoder.msRead > 300) {
                do {
                    t2 = System.currentTimeMillis()
                    dt = decoder.msTotal - (t2 - t1)
                    sleep(10)
                } while (dt > 200)
                decoder.msRead = 0f
            }
        }
    }

    private fun nextTrack() {
        uiController.nextTrack()
    }
}