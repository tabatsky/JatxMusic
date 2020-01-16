package jatx.musictransmitter.android.presentation

import android.util.Log
import jatx.debug.logError
import moxy.InjectViewState
import moxy.MvpPresenter
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import java.io.UnsupportedEncodingException
import javax.inject.Inject

@InjectViewState
class MusicEditorPresenter @Inject constructor() : MvpPresenter<MusicEditorView>() {
    private lateinit var file: File
    private lateinit var artist: String
    private lateinit var album: String
    private lateinit var title: String
    private lateinit var year: String
    private lateinit var number: String

    private var isWin = false

    fun onPathParsed(path: String) {
        file = File(path)

        viewState.showFileName(file.name)
        openTags()
    }

    fun onSaveClick() {
        viewState.saveTags()
    }

    fun onBackPressed(artist: String, album: String, title: String, year: String, number: String) {
        if (wasChanged(artist, album, title, year, number)) {
            viewState.showNeedToSaveDialog()
        } else {
            viewState.quit()
        }
    }

    fun onNeedQuit() {
        viewState.quit()
    }

    fun onSaveTags(artist: String, album: String, title: String, year: String, number: String) {
        try {
            this.artist = artist
            this.album = album
            this.title = title
            this.year = year
            this.number = number

            val mp3f = MP3File(file)

            val tag: Tag = mp3f.createDefaultTag()

            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM_ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.YEAR, year)
            tag.setField(FieldKey.TRACK, correctNumber(number))
            tag.setField(FieldKey.COMMENT, "tag created with jatx music tag editor")

            mp3f.tag = tag
            mp3f.save(file)
        } catch (e: Throwable) {
            logError(e)
        }
    }

    fun onWinToUtfClick(artist: String, album: String, title: String) {
        if (isWin) return

        try {
            val artistBytes = artist.toByteArray(charset("ISO-8859-1"))
            val albumBytes = album.toByteArray(charset("ISO-8859-1"))
            val titleBytes = title.toByteArray(charset("ISO-8859-1"))
            val artistWin = String(artistBytes, charset("WINDOWS-1251"))
            val albumWin = String(albumBytes, charset("WINDOWS-1251"))
            val titleWin = String(titleBytes, charset("WINDOWS-1251"))

            viewState.showTags(artistWin, albumWin, titleWin, year, number)
            isWin = true
        } catch (e: UnsupportedEncodingException) {
            Log.i("error", "unsupported encoding")
        }
    }

    private fun openTags() {
        try {
            val mp3f = MP3File(file)
            val tag: Tag = mp3f.tagOrCreateDefault
            artist = tag.getFirst(FieldKey.ARTIST)
            album = tag.getFirst(FieldKey.ALBUM)
            title = tag.getFirst(FieldKey.TITLE)
            year = tag.getFirst(FieldKey.YEAR)
            number = tag.getFirst(FieldKey.TRACK)

            viewState.showTags(artist, album, title, year, number)
        } catch (e: Throwable) {
            logError(e)
        }
    }

    private fun correctNumber(num: String): String? {
        return try {
            num.toInt()
            num
        } catch (e: NumberFormatException) {
            "0"
        }
    }

    private fun wasChanged(artist: String, album: String, title: String, year: String, number: String): Boolean {
        val noChanges =
            (this.artist == artist)
                .and(this.album == album)
                .and(this.title == title)
                .and(this.year == year)
                .and(this.number == number)
        return !noChanges
    }
}