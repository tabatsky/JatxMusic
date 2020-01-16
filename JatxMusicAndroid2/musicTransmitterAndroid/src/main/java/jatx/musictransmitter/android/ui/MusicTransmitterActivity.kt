package jatx.musictransmitter.android.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.obsez.android.lib.filechooser.ChooserDialog
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.Lazy
import jatx.constants.*
import jatx.debug.AppDebug
import jatx.extensions.onSeek
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.db.entity.Track
import jatx.extensions.showToast
import jatx.musictransmitter.android.presentation.MusicTransmitterPresenter
import jatx.musictransmitter.android.presentation.MusicTransmitterView
import kotlinx.android.synthetic.main.activity_music_transmitter.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import javax.inject.Inject

const val REQUEST_TAG_EDITOR = 2222

class MusicTransmitterActivity : MvpAppCompatActivity(), MusicTransmitterView {
    @Inject
    lateinit var presenterProvider: Lazy<MusicTransmitterPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    private val tracksAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppDebug.setAppCrashHandler()
        App.appComponent?.injectMusicTransmitterActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_transmitter)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        initTracksRV()

        playBtn.setOnClickListener { presenter.onPlayClick() }
        pauseBtn.setOnClickListener { presenter.onPauseClick() }

        repeatBtn.setOnClickListener { presenter.onRepeatClick() }
        shuffleBtn.setOnClickListener { presenter.onShuffleClick() }

        revBtn.setOnClickListener { presenter.onRevClick() }
        fwdBtn.setOnClickListener { presenter.onFwdClick() }

        volumeDownBtn.setOnClickListener { presenter.onVolumeDownClick() }
        volumeUpBtn.setOnClickListener { presenter.onVolumeUpClick() }

        seekBar.max = 1000
        seekBar.onSeek { i -> presenter.onProgressChanged(if (i < 1000) (i / 1000.0) else 0.999) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_music_transmitter, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_menu_add_track -> {
                presenter.onAddTrackSelected()
                true
            }
            R.id.item_menu_add_folder -> {
                presenter.onAddFolderSelected()
                true
            }
            R.id.item_menu_add_mic -> {
                presenter.onAddMicSelected()
                true
            }
            R.id.item_menu_remove_track -> {
                presenter.onRemoveTrackSelected()
                true
            }
            R.id.item_menu_remove_all -> {
                presenter.onRemoveAllTracksSelected()
                true
            }
            R.id.item_show_manual -> {
                presenter.onShowManualSelected()
                true
            }
            R.id.item_show_my_ip -> {
                presenter.onShowIPSelected()
                true
            }
            R.id.item_review_app -> {
                presenter.onReviewAppSelected()
                true
            }
            R.id.item_receiver_android -> {
                presenter.onReceiverAndroidSelected()
                true
            }
            R.id.item_receiver_javafx -> {
                presenter.onReceiverFXSelected()
                true
            }
            R.id.item_transmitter_javafx -> {
                presenter.onTransmitterFXSelected()
                true
            }
            R.id.item_source_code -> {
                presenter.onSourceCodeSelected()
                true
            }
            R.id.item_dev_site -> {
                presenter.onDevSiteSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TAG_EDITOR && resultCode == Activity.RESULT_OK) {
            presenter.onReturnFromTagEditor()
        }
    }

    override fun onBackPressed() = presenter.onBackPressed()

    override fun showTracks(tracks: List<Track>, currentPosition: Int) {
        val trackItems = tracks.mapIndexed { index, track ->
            TrackItem(track, index, index == currentPosition)
        }
        runOnUiThread {
            tracksAdapter.update(trackItems)
        }
    }

    override fun scrollToPosition(position: Int) {
        val layoutManager = tracksRV.layoutManager
        if (layoutManager is LinearLayoutManager) {
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }

    override fun showWifiStatus(isWifiOk: Boolean) {
        if (isWifiOk) {
            wifiNoIV.visibility = View.GONE
            wifiOkIV.visibility = View.VISIBLE
        } else {
            wifiNoIV.visibility = View.VISIBLE
            wifiOkIV.visibility = View.GONE
        }
    }

    override fun showPlayingState(isPlaying: Boolean) {
        if (isPlaying) {
            playBtn.visibility = View.GONE
            pauseBtn.visibility = View.VISIBLE
        } else {
            playBtn.visibility = View.VISIBLE
            pauseBtn.visibility = View.GONE
        }
    }

    override fun showShuffleState(isShuffle: Boolean) {
        if (isShuffle) {
            repeatBtn.visibility = View.GONE
            shuffleBtn.visibility = View.VISIBLE
        } else {
            repeatBtn.visibility = View.VISIBLE
            shuffleBtn.visibility = View.GONE
        }
    }

    override fun showOpenTrackDialog(initPath: String) {
        ChooserDialog(this)
            .withFilter(false, false, "mp3")
            .withStartFile(initPath)
            .withChosenListener { path, _ ->
                presenter.onTrackOpened(path)
            }
            .withOnCancelListener { dialog ->
                dialog.cancel()
            }
            .build()
            .show()
    }

    override fun showOpenFolderDialog(initPath: String) {
        ChooserDialog(this)
            .withFilter(true, false)
            .withStartFile(initPath)
            .withChosenListener { path, _ ->
                presenter.onFolderOpened(path)
            }
            .withOnCancelListener { dialog ->
                dialog.cancel()
            }
            .build()
            .show()
    }

    override fun showCurrentTime(currentMs: Float, trackLengthMs: Float) {
        if (trackLengthMs <= 0) return
        val progress = (currentMs * 1000 / trackLengthMs).toInt()
        seekBar.progress = progress
    }

    override fun showVolume(volume: Int) {
        volumeValueTV.text = "$volume%"
    }

    override fun showRemoveTrackMessage() {
        showToast(R.string.toast_long_tap)
    }

    override fun showTrackLongClickDialog(position: Int) {
        val dialog = TrackLongClickDialog()
        dialog.onRemoveThisTrack = { presenter.onDeleteTrack(position) }
        dialog.onOpenTagEditor = { presenter.onOpenTagEditor(position) }
        dialog.show(supportFragmentManager, "longClickDialog")
    }

    override fun showIPAddress(ipAddress: String) {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.show_ip_title)
            .setMessage(ipAddress)
            .setNegativeButton(R.string.button_ok) { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun tryAddMic() {
        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {
                presenter.onAddMicPermissionsAccepted()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                showToast(R.string.toast_no_mic_access)
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setPermissions(Manifest.permission.RECORD_AUDIO)
            .check()
    }

    override fun showTagEditor(uri: Uri) {
        val intent = Intent()
        intent.setClass(this, MusicEditorActivity::class.java)
        intent.data = uri
        startActivityForResult(intent, REQUEST_TAG_EDITOR)
    }

    override fun showManual() {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.manual_title)
            .setMessage(R.string.manual_message)
            .setNegativeButton(R.string.button_ok) { dialog, which -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun showReviewAppActivity() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TRANSMITTER_MARKET_URL1)
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TRANSMITTER_MARKET_URL2)
                )
            )
        }
    }

    override fun showReceiverAndroidActivity() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(RECEIVER_MARKET_URL1)
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(RECEIVER_MARKET_URL2)
                )
            )
        }
    }

    override fun showReceiverFXActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(FX_RECEIVER_URL)
            )
        )
    }

    override fun showTransmitterFXActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(FX_TRANSMITTER_URL)
            )
        )
    }

    override fun showSourceCodeActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(SOURCE_CODE_URL)
            )
        )
    }

    override fun showDevSiteActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(DEV_SITE_URL)
            )
        )
    }

    override fun showQuitDialog() {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(getString(R.string.really_quit))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                dialog.dismiss()
                presenter.onQuit()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun quit() {
        finish()
    }

    private fun initTracksRV() {
        tracksRV.adapter = tracksAdapter
        tracksAdapter.setOnItemClickListener { item, _ ->
            if (item is TrackItem) {
                presenter.onTrackClick(item.position)
            }
        }
        tracksAdapter.setOnItemLongClickListener { item, _ ->
            if (item is TrackItem) {
                presenter.onTrackLongClick(item.position)
                true
            } else {
                false
            }
        }
    }
}
