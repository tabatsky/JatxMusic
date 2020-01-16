package jatx.musictransmitter.android

import android.app.Application
import jatx.musictransmitter.android.di.AppComponent
import jatx.musictransmitter.android.di.DaggerAppComponent

class App : Application() {
    companion object {
        var appComponent: AppComponent? = null
    }

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent
            .builder()
            .context(this)
            .build()
    }
}