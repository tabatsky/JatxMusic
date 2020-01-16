package jatx.musicreceiver.android.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import jatx.musicreceiver.android.data.Settings
import jatx.musicreceiver.android.services.MusicReceiverService
import jatx.musicreceiver.android.ui.MusicReceiverActivity
import jatx.musicreceiver.android.ui.SelectHostDialog
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

    fun injectMusicReceiverActivity(musicReceiverActivity: MusicReceiverActivity)

    fun injectSelectHostDialog(selectHostDialog: SelectHostDialog)

    fun injectMusicReceiverService(musicReceiverService: MusicReceiverService)
}

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideSettings(context: Context) = Settings(context)
}

interface AppDeps {
    fun context(): Context
}