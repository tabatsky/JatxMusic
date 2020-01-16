package jatx.musictransmitter.android.data

import android.content.Context
import android.os.Environment
import java.io.File

const val PREFS_NAME = "MusicTransmitterPreferences"

const val KEY_MUSIC_DIR = "musicDirPath"
const val KEY_FILE_LIST = "fileList"
const val KEY_VOLUME = "volume"

class Settings(
    context: Context
) {
    private val sp = context.getSharedPreferences(PREFS_NAME, 0)

    var currentMusicDirPath: String
        get() = sp.getString(KEY_MUSIC_DIR, null) ?: Environment.getExternalStorageDirectory().absolutePath
        set(value) {
            val editor = sp.edit()
            editor.putString(KEY_MUSIC_DIR, value)
            editor.commit()
        }

    var currentFileList: List<File>
        get() {
            val listStr = sp.getString(KEY_FILE_LIST, null) ?: ""
            return if (listStr.isEmpty())
                listOf()
            else
                listStr.split("\n").map { File(it) }
        }
        set(value) {
            val listStr = value.joinToString("\n")
            val editor = sp.edit()
            editor.putString(KEY_FILE_LIST, listStr)
            editor.commit()
        }

    var volume: Int
        get() = sp.getInt(KEY_VOLUME, 100)
        set(value) {
            val editor = sp.edit()
            editor.putInt(KEY_VOLUME, value)
            editor.commit()
        }
}