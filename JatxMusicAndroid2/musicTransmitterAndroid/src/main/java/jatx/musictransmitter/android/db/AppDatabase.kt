package jatx.musictransmitter.android.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import jatx.musictransmitter.android.db.dao.TrackDao
import jatx.musictransmitter.android.db.entity.Track

@Database(
    entities = [
        Track::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context): AppDatabase = instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also {
                Log.e("db", "building")
                instance = it
            }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            AppDatabase::class.java, "musictransmitter.db")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }
}