package jatx.musicreceiver.android.threads

import jatx.debug.exceptionToString
import jatx.musiccommons.frameFromInputStream
import jatx.musicreceiver.android.audio.SoundOut
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

const val CONNECT_PORT_PLAYER = 7171

class ReceiverPlayer(
    private val host: String,
    private val uiController: UIController,
    @Volatile private var soundOut: SoundOut
) : Thread() {

    @Volatile private var finishFlag = false
    @Volatile private var isPlaying = true
    @Volatile private var volume = 100

    fun setupFinishFlag() {
        if (finishFlag) return
        finishFlag = true
        interrupt()
    }

    fun play() {
        if (!isPlaying) {
            soundOut.play()
            isPlaying = true
        }
    }

    fun pause() {
        if (isPlaying) {
            soundOut.pause()
            isPlaying = false
        }
    }

    fun setVolume(volume: Int) {
        this.volume = volume
        soundOut.setVolume(volume)
    }

    override fun run() {
        var s: Socket? = null
        var inputStream: InputStream? = null

        try {
            println("(player) " + "thread start")
            val ipAddr = InetAddress.getByName(host)
            s = Socket()
            s.connect(InetSocketAddress(ipAddr, CONNECT_PORT_PLAYER), SOCKET_TIMEOUT)
            inputStream = s.getInputStream()
            var frameRate = 44100
            var channels = 2
            var position = 0
            restartPlayer(frameRate, channels)
            while (!finishFlag) {
                if (isPlaying) {
                    val f = frameFromInputStream(inputStream)
                    if (frameRate != f.freq || channels != f.channels || position != f.position) {
                        frameRate = f.freq
                        channels = f.channels
                        position = f.position
                        restartPlayer(frameRate, channels)
                    }
                    soundOut.write(f.data, 0, f.size)
                }
            }
        } catch (e: InterruptedException) {
            System.err.println("(player) thread interrupted")
        } catch (e: SocketTimeoutException) {
            System.err.println("(player) socket timeout")
        } catch (e: IOException) {
            System.err.println("(player) io exception")
        } catch (e: Exception) {
            System.err.println("(player) " + exceptionToString(e))
        } finally {
            println("(player) " + "thread finish")
            soundOut.destroy()
            inputStream?.close()
            s?.close()
            uiController.stopJob()
        }
    }

    private fun restartPlayer(frameRate: Int, channels: Int) {
        soundOut.renew(frameRate, channels)
        soundOut.setVolume(volume)
        println("(player) " + "player restarted")
        println("(player) " + "frame rate: $frameRate")
        println("(player) " + "channels: $channels")
    }
}