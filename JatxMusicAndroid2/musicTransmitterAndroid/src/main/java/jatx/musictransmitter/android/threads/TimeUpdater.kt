package jatx.musictransmitter.android.threads

import jatx.musictransmitter.android.audio.Mp3Decoder

class TimeUpdater(
    @Volatile private var uiController: UIController,
    @Volatile private var decoder: Mp3Decoder
): Thread() {

    override fun run() {
        try {
            while (true) {
                uiController.setCurrentTime(decoder.currentMs, decoder.trackLengthSec * 1000f)
                sleep(500)
            }
        } catch (e: InterruptedException) {
            println("time updater interrupted")
        }
    }
}