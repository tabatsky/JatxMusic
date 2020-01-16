package jatx.musicreceiver.android.threads

import android.util.Log
import jatx.debug.exceptionToString
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

const val CONNECT_PORT_CONTROLLER = 7172

const val SOCKET_TIMEOUT = 1500

const val COMMAND_EMPTY = 255.toByte()
const val COMMAND_STOP = 127.toByte()
const val COMMAND_PAUSE = 126.toByte()
const val COMMAND_PLAY = 125.toByte()

class ReceiverController(
    private val host: String,
    private val uiController: UIController
) : Thread() {

    var rp: ReceiverPlayer? = null

    @Volatile private var finishFlag = false

    fun setupFinishFlag() {
        if (finishFlag) return
        finishFlag = true
        interrupt()
    }

    override fun run() {
        var s: Socket? = null
        var inputStream: InputStream? = null

        try {
            println("(controller) thread start")
            val ipAddr = InetAddress.getByName(host)
            s = Socket()
            s.connect(InetSocketAddress(ipAddr, CONNECT_PORT_CONTROLLER), SOCKET_TIMEOUT)
            inputStream = s.getInputStream()
            var numBytesRead: Int
            val data = ByteArray(1)
            var cmdSkipped = 0
            while (!finishFlag) {
                if (inputStream.available() > 0) {
                    cmdSkipped = 0
                    numBytesRead = inputStream.read(data, 0, 1)
                    if (numBytesRead == 1) {
                        when (val cmd = data[0]) {
                            in 0..100 -> {
                                setVolume(cmd.toInt())
                            }
                            COMMAND_PLAY -> {
                                play()
                            }
                            COMMAND_PAUSE -> {
                                pause()
                            }
                            COMMAND_STOP -> {
                                println("(controller) stop command received")
                                uiController.stopJob()
                            }
                        }
                    }
                } else {
                    sleep(50)
                    cmdSkipped++
                    if (cmdSkipped > 7) {
                        Log.e("cmd skipped", cmdSkipped.toString())
                        finishFlag = true
                    }
                }
            }
        } catch (e: InterruptedException) {
            System.err.println("(controller) thread interrupted")
        } catch (e: SocketTimeoutException) {
            System.err.println("(controller) socket timeout")
        } catch (e: IOException) {
            System.err.println("(controller) io exception")
            e.printStackTrace(System.err)
        } catch (e: Exception) {
            System.err.println("(controller) " + exceptionToString(e))
        } finally {
            println("(controller) " + "thread finish")
            inputStream?.close()
            s?.close()
            uiController.stopJob()
        }
    }

    private fun play() {
        println("(controller) play command received")
        rp?.play()
    }

    private fun pause() {
        println("(controller) pause command received")
        rp?.pause()
    }

    private fun setVolume(volume: Int) {
        println("(controller) volume command received: $volume")
        rp?.setVolume(volume)
    }
}