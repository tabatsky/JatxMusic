package jatx.musicreceiver.android.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import jatx.musicreceiver.android.R
import jatx.musicreceiver.android.data.Settings
import jatx.musicreceiver.android.services.MusicReceiverService
import jatx.musicreceiver.android.services.SERVICE_START_JOB
import jatx.musicreceiver.android.services.SERVICE_STOP_JOB
import jatx.musicreceiver.android.services.STOP_SERVICE
import moxy.InjectViewState
import moxy.MvpPresenter
import javax.inject.Inject

var UI_START_JOB = "jatx.musicreceiver.android.UI_START_JOB"
var UI_STOP_JOB = "jatx.musicreceiver.android.UI_STOP_JOB"


@InjectViewState
class MusicReceiverPresenter @Inject constructor(
    private val context: Context,
    private val settings: Settings
): MvpPresenter<MusicReceiverView>() {
    private lateinit var uiStartJobReceiver: BroadcastReceiver
    private lateinit var uiStopJobReceiver: BroadcastReceiver

    private var isRunning = false

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        initBroadcastReceivers()
        viewState.showSelectHostDialog()
    }

    override fun onDestroy() {
        stopService()
        unregisterReceivers()
        super.onDestroy()
    }

    fun onToggleClick() {
        if (!isRunning) {
            serviceStartJob()
        } else {
            serviceStopJob()
        }
    }

    fun onAutoConnectClick(checked: Boolean) {
        settings.isAutoConnect = checked
    }

    fun onBackPressed() = viewState.showQuitDialog()

    fun onQuit() = viewState.quit()

    fun onDialogOkClick() {
        prepareAndStart()
    }

    fun onDialogExitClick() = viewState.quit()

    fun onReviewAppSelected() = viewState.showReviewAppActivity()

    fun onTransmitterAndroidSelected() = viewState.showTransmitterAndroidActivity()

    fun onReceiverFXSelected() = viewState.showReceiverFXActivity()

    fun onTransmitterFXSelected() = viewState.showTransmitterFXActivity()

    fun onSourceCodeSelected() = viewState.showSourceCodeActivity()

    fun onDevSiteSelected() = viewState.showDevSiteActivity()

    private fun prepareAndStart() {
        viewState.showHost(settings.host)
        viewState.showAutoConnect(settings.isAutoConnect)
        startService()
    }

    private fun startService() {
        val intent = Intent(context, MusicReceiverService::class.java)
        context.startService(intent)
    }

    private fun stopService() {
        val intent = Intent(STOP_SERVICE)
        context.sendBroadcast(intent)
    }

    private fun uiStartJob() {
        if (isRunning) return
        isRunning = true
        viewState.showToggleText(context.getString(R.string.string_stop))
    }
    
    private fun uiStopJob() {
        if (!isRunning) return
        isRunning = false
        viewState.showToggleText(context.getString(R.string.string_start))
        viewState.showDisconnectOccured()
    }

    private fun serviceStartJob() {
        val intent = Intent(SERVICE_START_JOB)
        context.sendBroadcast(intent)
    }

    private fun serviceStopJob() {
        val intent = Intent(SERVICE_STOP_JOB)
        context.sendBroadcast(intent)
    }

    private fun initBroadcastReceivers() {
        uiStartJobReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                uiStartJob()
            }
        }
        context.registerReceiver(uiStartJobReceiver, IntentFilter(UI_START_JOB))

        uiStopJobReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                uiStopJob()
            }
        }
        context.registerReceiver(uiStopJobReceiver, IntentFilter(UI_STOP_JOB))
    }

    private fun unregisterReceivers() {
        context.unregisterReceiver(uiStartJobReceiver)
        context.unregisterReceiver(uiStopJobReceiver)
    }
}