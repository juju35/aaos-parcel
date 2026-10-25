package me.jstephan.parcel.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.jstephan.parcel.R

/** Parcel's status_code values. */
enum class Status(val code: Int, @StringRes val label: Int, @DrawableRes val icon: Int) {
    DELIVERED(0, R.string.status_delivered, R.drawable.ic_check),
    FROZEN(1, R.string.status_frozen, R.drawable.ic_pause),
    IN_TRANSIT(2, R.string.status_in_transit, R.drawable.ic_truck),
    AWAITING_PICKUP(3, R.string.status_awaiting_pickup, R.drawable.ic_store),
    OUT_FOR_DELIVERY(4, R.string.status_out_for_delivery, R.drawable.ic_truck),
    NOT_FOUND(5, R.string.status_not_found, R.drawable.ic_help),
    FAILED_ATTEMPT(6, R.string.status_failed_attempt, R.drawable.ic_error),
    EXCEPTION(7, R.string.status_exception, R.drawable.ic_warning),
    INFO_RECEIVED(8, R.string.status_info_received, R.drawable.ic_parcel),
    UNKNOWN(-1, R.string.status_unknown, R.drawable.ic_parcel);

    companion object {
        fun of(code: Int?): Status = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** One tracking event, as the carrier wrote it. */
data class TrackingEvent(val text: String, val date: String, val location: String, val additional: String)

data class Delivery(
    val carrierCode: String,
    val trackingNumber: String,
    val description: String,
    val status: Status,
    /** Newest first. */
    val events: List<TrackingEvent>,
    val extraInformation: String,
    /** Expected delivery window, epoch seconds, when Parcel gives one. */
    val expectedFrom: Long?,
    val expectedTo: Long?,
    /** The same window as text, used when Parcel has no timestamps. */
    val dateExpected: String?,
    val dateExpectedEnd: String?
) {
    val key: String get() = "$carrierCode:$trackingNumber"
    val title: String get() = description.ifBlank { trackingNumber }
    val lastEvent: TrackingEvent? get() = events.firstOrNull()

    /**
     * Changes whenever Parcel has news about the delivery: a new status or a new latest event. Not the number of
     * events, which changes when Parcel trims old ones.
     */
    val signature: String get() = listOf(status.code, lastEvent?.text, lastEvent?.date, lastEvent?.location).joinToString("|")
}
