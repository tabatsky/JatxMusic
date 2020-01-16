package jatx.musictransmitter.android.presentation

import android.net.Uri
import jatx.musictransmitter.android.db.entity.Track
import moxy.MvpView
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface MusicTransmitterView: MvpView {
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTracks(tracks: List<Track>, currentPosition: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun scrollToPosition(position: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showWifiStatus(isWifiOk: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showPlayingState(isPlaying: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showShuffleState(isShuffle: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showOpenTrackDialog(initPath: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showOpenFolderDialog(initPath: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showCurrentTime(currentMs: Float, trackLengthMs: Float)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showVolume(volume: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showRemoveTrackMessage()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTrackLongClickDialog(position: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showIPAddress(ipAddress: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun tryAddMic()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTagEditor(uri: Uri)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showManual()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showReviewAppActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showReceiverAndroidActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showReceiverFXActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTransmitterFXActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showSourceCodeActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showDevSiteActivity()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showQuitDialog()
    @StateStrategyType(SkipStrategy::class) fun quit()
}