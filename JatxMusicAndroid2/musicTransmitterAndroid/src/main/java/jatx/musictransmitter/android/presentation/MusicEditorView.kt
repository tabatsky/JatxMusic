package jatx.musictransmitter.android.presentation

import jatx.musictransmitter.android.db.entity.Track
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface MusicEditorView: MvpView {
    @StateStrategyType(AddToEndSingleStrategy::class) fun showFileName(fileName: String)
    @StateStrategyType(AddToEndSingleStrategy::class) fun showTags(
        artist: String, album: String, title: String, year: String, number: String
    )
    @StateStrategyType(AddToEndSingleStrategy::class) fun saveTags()
    @StateStrategyType(AddToEndSingleStrategy::class) fun showNeedToSaveDialog()
    @StateStrategyType(SkipStrategy::class) fun quit()
}