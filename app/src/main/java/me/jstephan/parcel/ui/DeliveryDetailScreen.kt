package me.jstephan.parcel.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.jstephan.parcel.R
import me.jstephan.parcel.data.Delivery
import me.jstephan.parcel.data.DeliveryRepository
import me.jstephan.parcel.data.Status
import me.jstephan.parcel.data.TrackingEvent

/**
 * One delivery: status, expected date, carrier and tracking number, then its events, newest first. Opened from the
 * list with the delivery, or from a notification with its key only (read from the last check).
 */
class DeliveryDetailScreen(carContext: CarContext, private val key: String, private var delivery: Delivery? = null) :
    Screen(carContext) {
    private val repo = DeliveryRepository.get(carContext)
    private var missing = false

    init {
        if (delivery == null) {
            lifecycleScope.launch {
                delivery = repo.cached()?.firstOrNull { it.key == key }
                missing = delivery == null
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val d = delivery
        if (d == null && missing) return Ui.message(carContext, R.string.delivery_missing, R.drawable.ic_help, Action.BACK)

        @Suppress("DEPRECATION") // Header needs car API 7; Android 12 hosts don't have it.
        val builder = ListTemplate.Builder()
            .setTitle(d?.title ?: carContext.getString(R.string.app_name))
            .setHeaderAction(Action.BACK)
        if (d == null) return builder.setLoading(true).build()
        pickupPlace(d)?.let { place ->
            val go = Action.Builder()
                .setTitle(carContext.getString(R.string.navigate))
                .setIcon(Ui.icon(carContext, R.drawable.ic_navigate))
                .setOnClickListener { navigate(place) }
                .build()
            builder.setActionStrip(ActionStrip.Builder().addAction(go).build())
        }
        return builder.setSingleList(itemList(d)).build()
    }

    private fun itemList(d: Delivery): ItemList {
        val rows = mutableListOf(summaryRow(d))
        if (d.extraInformation.isNotEmpty()) {
            rows += Row.Builder().setTitle(carContext.getString(R.string.extra_information)).addText(d.extraInformation).build()
        }
        if (d.events.isEmpty()) {
            rows += Row.Builder().setTitle(carContext.getString(R.string.no_events)).build()
        } else {
            d.events.mapTo(rows, ::eventRow)
        }
        val items = ItemList.Builder()
        rows.take(Ui.listLimit(carContext)).forEach { items.addItem(it) }
        return items.build()
    }

    /** "En transit" in its colour; then the expected date; then "Colissimo · 6A123…". */
    private fun summaryRow(d: Delivery): Row {
        val row = Row.Builder()
            .setTitle(carContext.getString(d.status.label))
            .setImage(Ui.statusIcon(carContext, d.status))
        if (d.status != Status.DELIVERED) Ui.expected(carContext, d)?.let(row::addText)
        row.addText(Ui.join(repo.carrierName(d.carrierCode), d.trackingNumber))
        return row.build()
    }

    private fun eventRow(event: TrackingEvent): Row {
        val row = Row.Builder().setTitle(event.text.ifEmpty { event.date.ifEmpty { "–" } })
        Ui.join(event.date, event.location).takeIf { it.isNotEmpty() }?.let(row::addText)
        if (event.additional.isNotEmpty()) row.addText(event.additional)
        return row.build()
    }

    /** Where to pick the parcel up, as the carrier gave it in the last event. */
    private fun pickupPlace(d: Delivery): String? =
        if (d.status == Status.AWAITING_PICKUP) d.lastEvent?.location?.takeIf { it.isNotEmpty() } else null

    private fun navigate(place: String) {
        try {
            carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:0,0?q=" + Uri.encode(place))))
        } catch (e: Exception) {
            Log.w(TAG, "No navigation app", e)
            Ui.toast(carContext, R.string.navigation_unavailable)
        }
    }

    private companion object {
        const val TAG = "DeliveryDetail"
    }
}
