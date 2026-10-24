package me.jstephan.parcel.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import me.jstephan.parcel.R

/**
 * First screen while there is no API key. Replaced by the list once a key is saved: the host refuses a screen
 * that comes back with another template type, so the list is a screen of its own.
 */
class SetupScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val configure = Ui.action(carContext, R.string.configure, primary = true) {
            screenManager.pushForResult(ApiKeyScreen(carContext)) { saved ->
                if (saved == true) {
                    screenManager.push(DeliveryListScreen(carContext))
                    finish()
                }
            }
        }
        return Ui.message(carContext, R.string.setup_message, R.drawable.ic_key, Action.APP_ICON, configure)
    }
}
