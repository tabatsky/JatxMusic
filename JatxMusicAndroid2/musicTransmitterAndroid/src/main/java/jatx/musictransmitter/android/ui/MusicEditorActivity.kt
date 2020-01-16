package jatx.musictransmitter.android.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import dagger.Lazy
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.presentation.MusicEditorPresenter
import jatx.musictransmitter.android.presentation.MusicEditorView
import kotlinx.android.synthetic.main.activity_music_editor.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import javax.inject.Inject

class MusicEditorActivity : MvpAppCompatActivity(), MusicEditorView {
    @Inject
    lateinit var presenterProvider: Lazy<MusicEditorPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent?.injectMusicEditorActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_editor)

        val path = intent.data?.path
        path?.apply {
            presenter.onPathParsed(this)
        }

        saveBtn.setOnClickListener {
            presenter.onSaveClick()
        }

        winToUtfBtn.setOnClickListener {
            presenter.onWinToUtfClick(
                artist = artistET.text.toString(),
                album = albumET.text.toString(),
                title = titleET.text.toString()
            )
        }
    }

    override fun onBackPressed() {
        presenter.onBackPressed(
            artist = artistET.text.toString(),
            album = albumET.text.toString(),
            title = titleET.text.toString(),
            year = yearET.text.toString(),
            number = numberET.text.toString()
        )
    }

    override fun showFileName(fileName: String) {
        fileNameTV.text = "File: $fileName"
    }

    override fun showTags(
        artist: String,
        album: String,
        title: String,
        year: String,
        number: String
    ) {
        artistET.setText(artist)
        albumET.setText(album)
        titleET.setText(title)
        yearET.setText(year)
        numberET.setText(number)
    }

    override fun saveTags() {
        presenter.onSaveTags(
            artist = artistET.text.toString(),
            album = albumET.text.toString(),
            title = titleET.text.toString(),
            year = yearET.text.toString(),
            number = numberET.text.toString()
        )
    }

    override fun showNeedToSaveDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Сохранить изменения?")
            .setPositiveButton("Да") { dialog, _ ->
                presenter.onSaveClick()
                dialog.dismiss()
                presenter.onNeedQuit()
            }
            .setNegativeButton(
                "Нет") { dialog, _ ->
                dialog.dismiss()
                presenter.onNeedQuit()
            }
            .create()
            .show()
    }

    override fun quit() {
        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }
}