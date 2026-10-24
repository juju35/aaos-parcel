package me.jstephan.parcel.ui

import android.text.format.DateUtils
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import me.jstephan.parcel.FilterMode
import me.jstephan.parcel.Prefs
import me.jstephan.parcel.R

/** API key, deliveries listed, notifications. */
class SettingsScreen(carContext: CarContext) : Screen(carContext) {
    private val prefs = Prefs(carContext)

    @Suppress("DEPRECATION") // Header needs car API 7; Android 12 hosts don't have it.
    override fun onGetTemplate(): Template {
        val items = ItemList.Builder()
            .addItem(keyRow())
            .addItem(filterRow())
            .addItem(notificationsRow())
            .build()
        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.settings))
            .setHeaderAction(Action.BACK)
            .setSingleList(items)
            .build()
    }

    private fun keyRow(): Row = Row.Builder()
        .setTitle(carContext.getString(R.string.api_key_title))
        .addText(carContext.getString(if (prefs.hasKey) R.string.api_key_set else R.string.api_key_missing))
        .setImage(Ui.icon(carContext, R.drawable.ic_key))
        .setOnClickListener { screenManager.push(ApiKeyScreen(carContext)) }
        .build()

    private fun filterRow(): Row = Row.Builder()
        .setTitle(carContext.getString(R.string.filter_title))
        .addText(carContext.getString(if (prefs.filter == FilterMode.RECENT) R.string.filter_recent else R.string.filter_active))
        .setImage(Ui.icon(carContext, R.drawable.ic_filter))
        .setOnClickListener {
            prefs.filter = if (prefs.filter == FilterMode.RECENT) FilterMode.ACTIVE else FilterMode.RECENT
            invalidate()
        }
        .build()

    /** "Vérification toutes les heures", then "Vérifié il y a 12 min". */
    private fun notificationsRow(): Row {
        val toggle = Toggle.Builder { checked ->
            prefs.notifications = checked
            if (checked) Ui.requestNotifications(carContext, ::invalidate) else invalidate()
        }.setChecked(prefs.notifications).build()
        val row = Row.Builder()
            .setTitle(carContext.getString(R.string.notifications_title))
            .addText(carContext.getString(R.string.check_hourly))
            .setImage(Ui.icon(carContext, R.drawable.ic_notifications))
            .setToggle(toggle)
        val last = prefs.lastCheck
        if (last > 0) {
            val now = System.currentTimeMillis()
            val ago = if (now - last < DateUtils.MINUTE_IN_MILLIS) {
                carContext.getString(R.string.just_now)
            } else {
                DateUtils.getRelativeTimeSpanString(last, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
            }
            row.addText(carContext.getString(R.string.last_check, ago))
        }
        return row.build()
    }
}
