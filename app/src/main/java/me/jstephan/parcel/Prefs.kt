package me.jstephan.parcel

import android.content.Context

/** Deliveries listed: those under way and recently delivered, or only those under way. */
enum class FilterMode { RECENT, ACTIVE }

/**
 * Settings kept between drives. The API key and the check state live in a second file that backups leave
 * out (xml/data_extraction_rules.xml): a key stays on the car it was typed in.
 */
class Prefs(context: Context) {
    private val store = context.getSharedPreferences("parcel", Context.MODE_PRIVATE)
    private val device = context.getSharedPreferences("parcel_device", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = device.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) = device.edit().putString(KEY_API_KEY, value).apply()

    val hasKey: Boolean get() = apiKey != null

    var filter: FilterMode
        get() = FilterMode.entries.firstOrNull { it.name == store.getString(KEY_FILTER, null) } ?: FilterMode.RECENT
        set(value) = store.edit().putString(KEY_FILTER, value.name).apply()

    var notifications: Boolean
        get() = store.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = store.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    /** Wall-clock time of the last successful check, 0 before the first one. */
    var lastCheck: Long
        get() = device.getLong(KEY_LAST_CHECK, 0L)
        set(value) = device.edit().putLong(KEY_LAST_CHECK, value).apply()

    /** The refused key was already notified: once per key, not every hour. */
    var authFailureNotified: Boolean
        get() = device.getBoolean(KEY_AUTH_NOTIFIED, false)
        set(value) = device.edit().putBoolean(KEY_AUTH_NOTIFIED, value).apply()

    private companion object {
        const val KEY_API_KEY = "api_key"
        const val KEY_FILTER = "filter"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_LAST_CHECK = "last_check"
        const val KEY_AUTH_NOTIFIED = "auth_failure_notified"
    }
}
