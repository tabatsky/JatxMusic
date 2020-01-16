package jatx.musicreceiver.android

import android.app.Application
import jatx.musicreceiver.android.di.AppComponent
import jatx.musicreceiver.android.di.DaggerAppComponent

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