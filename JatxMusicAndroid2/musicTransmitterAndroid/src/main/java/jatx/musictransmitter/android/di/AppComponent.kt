package jatx.musictransmitter.android.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import jatx.musictransmitter.android.data.Settings
import jatx.musictransmitter.android.data.TrackInfoStorage
import jatx.musictransmitter.android.db.AppDatabase
import jatx.musictransmitter.android.db.dao.TrackDao
import jatx.musictransmitter.android.services.MusicTransmitterService
import jatx.musictransmitter.android.ui.MusicEditorActivity
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent: AppDeps {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }

    fun injectMusicTransmitterActivity(musicTransmitterActivity: MusicTransmitterActivity)

    fun injectMusicTransmitterService(musicTransmitterService: MusicTransmitterService)

    fun injectMusicEditorActivity(musicEditorActivity: MusicEditorActivity)
}

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideSettings(context: Context) = Settings(context)

    @Provides
    @Singleton
    fun provideTrackDao(context: Context) = AppDatabase.invoke(context).trackDao()

    @Provides
    @Singleton
    fun provideTrackInfoStorage(trackDao: TrackDao) = TrackInfoStorage(trackDao)
}

interface AppDeps {
    fun context(): Context
}