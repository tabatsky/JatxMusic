package jatx.musicreceiver.android.data

import android.content.Context

const val PREFS_NAME = "MusicTransmitterPreferences"

const val KEY_ALL_HOSTS = "allHosts"
const val KEY_HOST = "host"
const val KEY_AUTO_CONNECT = "isAutoConnect"

class Settings (
    context: Context
) {
    private val sp = context.getSharedPreferences(PREFS_NAME, 0)

    var allHosts: List<String>
        get() = (sp.getString(KEY_ALL_HOSTS, null) ?: "New Transmitter\n127.0.0.1").split("\n")
        set(value) {
            val editor = sp.edit()
            editor.putString(KEY_ALL_HOSTS, value.joinToString("\n"))
            editor.commit()
        }

    var host: String
        get() = sp.getString(KEY_HOST, null) ?: "127.0.0.1"
        set(value) {
            val editor = sp.edit()
            editor.putString(KEY_HOST, value)
            editor.commit()
        }

    var isAutoConnect: Boolean
        get() = sp.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) {
            val editor = sp.edit()
            editor.putBoolean(KEY_AUTO_CONNECT, value)
            editor.commit()
        }
}