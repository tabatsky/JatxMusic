package jatx.musicreceiver.android.presentation

import moxy.MvpView
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface MusicReceiverView: MvpView {
    @StateStrategyType(OneExecutionStateStrategy::class) fun showSelectHostDialog()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showToggleText(text: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showHost(host: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showAutoConnect(isAutoConnect: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showDisconnectOccured()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showReviewAppActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTransmitterAndroidActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showReceiverFXActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTransmitterFXActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showSourceCodeActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showDevSiteActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showQuitDialog()
    @StateStrategyType(SkipStrategy::class) fun quit()
}