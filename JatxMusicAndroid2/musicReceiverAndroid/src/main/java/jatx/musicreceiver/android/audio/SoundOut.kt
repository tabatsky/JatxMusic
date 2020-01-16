package jatx.musicreceiver.android.audio

interface SoundOut {
    fun renew(frameRate: Int, channels: Int)
    fun setVolume(volume: Int)
    fun write(data: ByteArray, offset: Int, size: Int)
    fun destroy()
    fun play()
    fun pause()
}