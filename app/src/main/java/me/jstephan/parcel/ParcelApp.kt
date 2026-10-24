package me.jstephan.parcel

import android.app.Application
import me.jstephan.parcel.work.CheckWorker
import me.jstephan.parcel.work.Notifier

class ParcelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        if (Prefs(this).hasKey) CheckWorker.schedule(this)
    }
}
