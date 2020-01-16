package jatx.musictransmitter.android.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import jatx.musictransmitter.android.db.entity.Track

@Dao
interface TrackDao {
    @Query("SELECT * FROM track_info WHERE path=:path")
    fun getTrack(path: String): Track?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putTrack(track: Track)
}