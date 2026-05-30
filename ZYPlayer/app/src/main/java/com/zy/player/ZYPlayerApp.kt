package com.zy.player

import android.app.Application
import com.zy.player.data.repository.AutoSourceCheckScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ZYPlayerApp : Application() {
    @Inject
    lateinit var autoSourceCheckScheduler: AutoSourceCheckScheduler

    override fun onCreate() {
        super.onCreate()
        autoSourceCheckScheduler.start()
    }
}
