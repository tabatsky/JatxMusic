package jatx.musicreceiver.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import jatx.extensions.onItemSelected
import jatx.extensions.setContent
import jatx.extensions.showToast
import jatx.musicreceiver.android.App
import jatx.musicreceiver.android.R
import jatx.musicreceiver.android.data.Settings
import javax.inject.Inject

class SelectHostDialog : DialogFragment() {
    @Inject
    lateinit var settings: Settings

    var onOk: () -> Unit = {}
    var onExit: () -> Unit = {}

    init {
        this.isCancelable = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent?.injectSelectHostDialog(this)

        super.onCreate(savedInstanceState)

        val style = STYLE_NORMAL
        val theme: Int = androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog

        setStyle(style, theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_select_host, container, false)

        val hostSpinner = v.findViewById<Spinner>(R.id.hostSpinner)
        val hostnameET = v.findViewById<EditText>(R.id.hostnameET)
        val okBtn = v.findViewById<Button>(R.id.okBtn)
        val exitBtn = v.findViewById<Button>(R.id.exitBtn)

        hostSpinner.setContent(settings.allHosts)
        hostSpinner.setSelection(settings.allHosts.indexOf(settings.host))

        hostSpinner.onItemSelected { position ->
            if (position > 0) {
                hostnameET.setText(settings.allHosts[position])
            } else {
                hostnameET.setText("")
            }
        }

        hostnameET.setText(settings.host)

        okBtn.setOnClickListener {
            val host = hostnameET.text.toString()

            if (host.isEmpty()) {
                context?.showToast(R.string.toast_empty_host)
                return@setOnClickListener
            }

            settings.host = host

            if (!settings.allHosts.contains(host)) {
                val allHosts = arrayListOf<String>()
                allHosts.addAll(settings.allHosts)
                allHosts.add(host)
                settings.allHosts = allHosts
            }

            dismiss()
            onOk()
        }

        exitBtn.setOnClickListener {
            dismiss()
            onExit()
        }

        return v
    }
}