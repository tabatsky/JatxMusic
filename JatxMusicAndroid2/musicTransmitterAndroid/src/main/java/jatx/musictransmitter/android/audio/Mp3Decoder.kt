package jatx.musictransmitter.android.audio

import jatx.musiccommons.Frame
import java.io.File
import java.lang.Exception

abstract class Mp3Decoder {
    var resetTimeFlag = true
    var disconnectResetTimeFlag = true

    var msRead = 0f
    var msTotal = 0f

    var currentMs = 0f
    var trackLengthSec = 0

    var position = 0

    abstract var file: File?

    abstract fun readFrame(): Frame?
    abstract fun seek(progress: Double)

    fun setPath(path: String) {
        file = File(path)
    }
}

class Mp3DecoderException: Exception {
    constructor(cause: Exception): super(cause)
    constructor(msg: String): super(msg)
}

class TrackFinishException: Exception()