package jatx.musictransmitter.android.audio

import jatx.debug.logError
import jatx.musiccommons.Frame
import jatx.musiccommons.WrongFrameException
import jatx.musiccommons.frameFromSampleBuffer
import jatx.musictransmitter.android.data.FileDoesNotExistException
import jatx.musictransmitter.android.data.MIC_PATH
import javazoom.jl.decoder.*
import org.jaudiotagger.audio.AudioFileIO
import java.io.*

class JLayerMp3Decoder : Mp3Decoder() {
    private var decoder: Decoder? = null
    private var bitstream: Bitstream? = null
    private var msFrame = 0f

    override var file: File? = null
        set(value) {
            if (value != null && value.absolutePath == MIC_PATH) {
                field = value
                return
            }

            if (value == null || !value.exists()) {
                throw Mp3DecoderException(FileDoesNotExistException())
            }

            field = value

            try {
                val af = AudioFileIO.read(value)
                trackLengthSec = af.audioHeader.trackLength
            } catch (e: Exception) {
                logError(e)
            }

            decoder = Decoder()

            bitstream?.apply {
                try {
                    this.close()
                } catch (e: BitstreamException) {
                    throw Mp3DecoderException(e)
                }
                bitstream = null
            }

            try {
                val inputStream: InputStream = BufferedInputStream(FileInputStream(value), 32 * 1024)
                bitstream = Bitstream(inputStream)
            } catch (e: FileNotFoundException) {
                throw Mp3DecoderException(e)
            }

            currentMs = 0f
            resetTimeFlag = true
        }

    override fun readFrame(): Frame? {
        var f: Frame? = null

        try {
            val frameHeader = (if (bitstream != null) {
                bitstream!!.readFrame()
            } else {
                throw Mp3DecoderException("bitstream: null")
            })
                ?: throw TrackFinishException()
            msFrame = frameHeader.ms_per_frame()
            msRead += msFrame
            msTotal += msFrame
            currentMs += msFrame
            val output = try {
                decoder!!.decodeFrame(frameHeader, bitstream)
            } catch (e: ArrayIndexOutOfBoundsException) { //Log.e("readFrame", "ArrayIndexOutOfBounds");
                bitstream!!.closeFrame()
                throw TrackFinishException()
            }
            if (output is SampleBuffer) {
                f = frameFromSampleBuffer(output, position)
            }
            bitstream!!.closeFrame()
        } catch (e: WrongFrameException) {
            throw e
        } catch (e: BitstreamException) {
            throw Mp3DecoderException(e)
        } catch (e: DecoderException) {
            throw Mp3DecoderException(e)
        }

        return f
    }

    override fun seek(progress: Double) {
        file = file
        try {
            while (currentMs < trackLengthSec * 1000.0 * progress) {
                val frameHeader = if (bitstream != null) {
                    bitstream!!.readFrame()
                } else {
                    throw Mp3DecoderException("bitstream: null")
                }
                if (frameHeader != null) {
                    msFrame = frameHeader.ms_per_frame()
                    msRead += msFrame
                    currentMs += msFrame
                    bitstream!!.closeFrame()
                } else {
                    println("frame header is null $currentMs")
                    break
                }
            }
        } catch (e: BitstreamException) {
            throw Mp3DecoderException(e)
        }

        resetTimeFlag = true
    }
}