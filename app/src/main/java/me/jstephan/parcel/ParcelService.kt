package me.jstephan.parcel

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import me.jstephan.parcel.ui.DeliveryDetailScreen
import me.jstephan.parcel.ui.DeliveryListScreen
import me.jstephan.parcel.work.Notifier

class ParcelService : CarAppService() {
    // The library's own list of trusted hosts, as in Google's samples.
    @SuppressLint("PrivateResource")
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(): Session = ParcelSession()
}

/** Opens on the list, or on a delivery when started from its notification. */
class ParcelSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val list = DeliveryListScreen(carContext)
        val key = intent.getStringExtra(Notifier.EXTRA_DELIVERY) ?: return list
        screenManager.push(list)
        return DeliveryDetailScreen(carContext, key)
    }

    override fun onNewIntent(intent: Intent) {
        val key = intent.getStringExtra(Notifier.EXTRA_DELIVERY) ?: return
        screenManager.popToRoot()
        screenManager.push(DeliveryDetailScreen(carContext, key))
    }
}
