package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import jatx.musictransmitter.android.R

class TrackLongClickDialog : DialogFragment() {
    var onRemoveThisTrack: () -> Unit = {}
    var onOpenTagEditor: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val style = DialogFragment.STYLE_NORMAL
        val theme = androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog

        setStyle(style, theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_track_edit_delete, container, false)

        val cancelBtn = v.findViewById<Button>(R.id.cancelBtn)
        val removeThisTrackBtn = v.findViewById<Button>(R.id.removeThisTrackBtn)
        val openTagEditorBtn = v.findViewById<Button>(R.id.openTagEditorBtn)

        cancelBtn.setOnClickListener { dismiss() }
        removeThisTrackBtn.setOnClickListener {
            onRemoveThisTrack()
            dismiss()
        }
        openTagEditorBtn.setOnClickListener {
            onOpenTagEditor()
            dismiss()
        }

        return v
    }
}