package jatx.musictransmitter.android.threads

import jatx.debug.logError
import jatx.musictransmitter.android.data.Settings
import java.io.IOException
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

const val CONNECT_PORT_CONTROLLER = 7172

const val COMMAND_EMPTY = 255.toByte()
const val COMMAND_STOP = 127.toByte()
const val COMMAND_PAUSE = 126.toByte()
const val COMMAND_PLAY = 125.toByte()

const val SO_TIMEOUT = 1000

class TransmitterController(
    private val initialVolume: Int
) : Thread() {
    @Volatile
    var finishFlag = false
    @Volatile
    var fifo: BlockingQueue<Byte> = ArrayBlockingQueue<Byte>(2048)

    @Volatile
    var forceDisconnectFlag = false

    var tp: TransmitterPlayer? = null

    private var ss: ServerSocket? = null
    private var os: OutputStream? = null

    fun play() {
        println("(controller) play")
        fifo.offer(COMMAND_PLAY)
    }

    fun pause() {
        println("(controller) pause")
        fifo.offer(COMMAND_PAUSE)
    }

    fun sendStop() {
        println("(controller) stop")
        fifo.offer(COMMAND_STOP)
    }

    fun setVolume(vol: Int) {
        println("(controller) set volume: " + Integer.valueOf(vol).toString())
        if (vol in 0..100) {
            fifo.offer(vol.toByte())
        }
    }

    override fun run() {
        println("(controller) thread started")
        try {
            while (!finishFlag) {
                sleep(100)
                ss = ServerSocket(CONNECT_PORT_CONTROLLER)
                println("(controller) new server socket")
                try {
                    ss?.soTimeout = SO_TIMEOUT
                    val s = ss?.accept()
                    os = s?.getOutputStream()
                    println("(controller) socket connect")
                    setVolume(initialVolume)
                    while (!finishFlag) {
                        val cmd = fifo.poll() ?: COMMAND_EMPTY
                        val data = byteArrayOf(cmd)
                        os?.write(data)
                        os?.flush()
                        sleep(10)
                        if (forceDisconnectFlag) {
                            println("(player) disconnect flag: throwing DisconnectException")
                            throw ForceDisconnectException()
                        }
                    }
                    val data = byteArrayOf(COMMAND_STOP)
                    os?.write(data)
                    os?.flush()
                } catch (e: SocketTimeoutException) {
                    println("(controller) socket timeout")
                } catch (e: ForceDisconnectException) {
                    println("(controller) socket force disconnect")
                    println("(controller) " + Date().time % 10000)
                } catch (e: IOException) {
                    println("(controller) socket disconnect")
                    println("(controller) " + Date().time % 10000)
                    tp?.forceDisconnectFlag = true
                    sleep(250)
                } finally {
                    forceDisconnectFlag = false
                    os?.close()
                    println("(controller) outstream closed")
                    ss?.close()
                    println("(controller) server socket closed")
                }
            }
        } catch (e: IOException) {
            logError(e)
        } catch (e: InterruptedException) {
            println("(controller) thread interrupted")
            os?.close()
            println("(controller) outstream closed")
            ss?.close()
            println("(controller) server socket closed")
        } finally {
            println("(controller) thread finished")
        }
    }
}