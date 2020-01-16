package jatx.musictransmitter.android.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import jatx.debug.logError
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import java.io.File

@Entity(tableName = "track_info")
data class Track(
    @PrimaryKey
    val path: String,
    var artist: String = "",
    var album: String = "",
    var title: String = "",
    var year: String = "1900",
    var length: String = "",
    var number: String = "0",
    @ColumnInfo(name = "last_modified")
    var lastModified: Long = 0
) {

    fun tryToFill(file: File) {
        try {
            val af = AudioFileIO.read(file)
            val len: Int = af.audioHeader.trackLength
            val sec = len % 60
            val min = (len - sec) / 60
            length = String.format("%02d:%02d", min, sec)
            val mp3f = MP3File(file)
            val tag = mp3f.getTag()
            artist = tag.getFirst(FieldKey.ARTIST).trim()
            album = tag.getFirst(FieldKey.ALBUM).trim()
            title = tag.getFirst(FieldKey.TITLE).trim()
            year = tag.getFirst(FieldKey.YEAR)
            number = tag.getFirst(FieldKey.TRACK)
            if (number != "") {
                val num: Int = number.toInt()
                if (num < 10) {
                    number = "00$num"
                } else if (num < 100) {
                    number = "0$num"
                }
            }
        } catch (e: Throwable) {
            logError(e)
        }
    }
}