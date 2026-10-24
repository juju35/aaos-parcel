package me.jstephan.parcel.ui

import android.text.SpannableStringBuilder
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.jstephan.parcel.FilterMode
import me.jstephan.parcel.Prefs
import me.jstephan.parcel.R
import me.jstephan.parcel.data.Delivery
import me.jstephan.parcel.data.DeliveryRepository
import me.jstephan.parcel.data.ParcelException
import me.jstephan.parcel.data.Status
import java.io.IOException

/** Home screen: the deliveries, those under way first, by expected date. */
class DeliveryListScreen(carContext: CarContext) : Screen(carContext) {
    private val repo = DeliveryRepository.get(carContext)
    private val prefs = Prefs(carContext)

    private var deliveries: List<Delivery>? = null
    private var error: Int? = null
    private var refreshJob: Job? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = load()
        })
    }

    /** The last check straight away, then a new one if it is older than [STALE_MS]. */
    private fun load() {
        if (!prefs.hasKey) {
            deliveries = null
            error = null
            invalidate()
            return
        }
        lifecycleScope.launch {
            deliveries = repo.cached()
            invalidate()
            if (deliveries == null || repo.isStale(STALE_MS)) refresh(manual = false)
        }
    }

    private fun refresh(manual: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = lifecycleScope.launch {
            try {
                deliveries = repo.refresh().deliveries
                error = null
                if (manual) CarToast.makeText(carContext, R.string.refreshed, CarToast.LENGTH_SHORT).show()
            } catch (e: ParcelException.Auth) {
                // Said even on an automatic refresh: the list shown won't change until the key does.
                failed(e, alwaysTell = true)
            } catch (e: IOException) {
                failed(e, alwaysTell = manual)
            }
            invalidate()
        }
    }

    /** No list yet: the error replaces it. Otherwise the last check stays, with a toast if [alwaysTell]. */
    private fun failed(e: IOException, alwaysTell: Boolean) {
        Log.w(TAG, "Refresh failed", e)
        if (deliveries == null) {
            error = Ui.errorMessage(e)
        } else if (alwaysTell) {
            Ui.toast(carContext, Ui.errorMessage(e))
        }
    }

    override fun onGetTemplate(): Template {
        if (!prefs.hasKey) {
            val configure = Ui.action(carContext, R.string.configure, primary = true) { screenManager.push(ApiKeyScreen(carContext)) }
            return Ui.message(carContext, R.string.setup_message, R.drawable.ic_key, Action.APP_ICON, configure)
        }
        val list = deliveries
        val err = error
        if (list == null && err != null) {
            val retry = Ui.action(carContext, R.string.retry, primary = true) {
                error = null
                refresh(manual = true)
                invalidate()
            }
            val settings = Ui.action(carContext, R.string.settings) { screenManager.push(SettingsScreen(carContext)) }
            return Ui.message(carContext, err, R.drawable.ic_error, Action.APP_ICON, retry, settings)
        }

        @Suppress("DEPRECATION") // Header needs car API 7; Android 12 hosts don't have it.
        val builder = ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.list_title))
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(Ui.iconAction(carContext, R.drawable.ic_refresh) { refresh(manual = true) })
                    .addAction(Ui.iconAction(carContext, R.drawable.ic_settings) { screenManager.push(SettingsScreen(carContext)) })
                    .build()
            )
        if (list == null) builder.setLoading(true) else builder.setSingleList(itemList(list))
        return builder.build()
    }

    private fun itemList(list: List<Delivery>): ItemList {
        val shown = list
            .filter { prefs.filter == FilterMode.RECENT || it.status != Status.DELIVERED }
            .sortedWith(compareBy<Delivery> { it.status == Status.DELIVERED }.thenBy { it.expectedFrom ?: Long.MAX_VALUE })
            .take(Ui.listLimit(carContext))
        val items = ItemList.Builder().setNoItemsMessage(carContext.getString(R.string.no_deliveries))
        shown.forEach { items.addItem(row(it)) }
        return items.build()
    }

    /**
     * Title: the description. Then the status in its colour with the expected date (the carrier once delivered),
     * then the last event and where it happened.
     */
    private fun row(delivery: Delivery): Row {
        val label = carContext.getString(delivery.status.label)
        val status = SpannableStringBuilder(Ui.colored(label, Ui.statusColor(delivery.status)))
        val detail = if (delivery.status == Status.DELIVERED) null else Ui.expected(carContext, delivery)
        (detail ?: repo.carrierName(delivery.carrierCode).takeIf { it.isNotBlank() })?.let { status.append(" · ").append(it) }

        val row = Row.Builder()
            .setTitle(delivery.title)
            .addText(status)
            .setImage(Ui.statusIcon(carContext, delivery.status))
            .setOnClickListener { screenManager.push(DeliveryDetailScreen(carContext, delivery.key, delivery)) }
        delivery.lastEvent?.let { event ->
            Ui.join(event.text, event.location).takeIf { it.isNotEmpty() }?.let(row::addText)
        }
        return row.build()
    }

    private companion object {
        const val TAG = "DeliveryList"

        // Opening the app checks again after 5 minutes: Parcel allows 20 requests an hour.
        const val STALE_MS = 5 * 60 * 1000L
    }
}
