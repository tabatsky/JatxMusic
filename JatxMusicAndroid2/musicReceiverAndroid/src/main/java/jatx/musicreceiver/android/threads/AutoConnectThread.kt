package jatx.musicreceiver.android.threads

import jatx.musicreceiver.android.data.Settings

class AutoConnectThread(
    @Volatile private var settings: Settings,
    @Volatile private var uiController: UIController
) : Thread() {

    override fun run() {
        try {
            while (true) {
                if (settings.isAutoConnect) {
                    uiController.startJob()
                }
                sleep(5000)
            }
        } catch (e: InterruptedException) {
            println("(auto connect thread) interrupted")
        }
    }
}