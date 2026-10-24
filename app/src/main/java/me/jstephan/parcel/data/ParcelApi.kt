package me.jstephan.parcel.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/** Why a Parcel call failed: the key, the hourly limit, or anything else (network, server, format). */
sealed class ParcelException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class Auth(message: String) : ParcelException(message)
    class RateLimit(message: String) : ParcelException(message)
    class Failure(message: String, cause: Throwable? = null) : ParcelException(message, cause)
}

/**
 * Parcel's external API, as ha-parcel uses it: key in the `api-key` header (generated at web.parcelapp.net,
 * Parcel Premium), deliveries limited to 20 requests per hour. Blocking: call it from Dispatchers.IO.
 */
object ParcelApi {
    private const val BASE = "https://api.parcel.app/external"

    /**
     * The deliveries call's JSON body, checked. Always the "recent" list (under way and recently delivered):
     * a delivery that turns delivered stays in it, so its last update can still be notified. The key "demo" gives
     * made-up deliveries (DemoData).
     */
    fun fetchDeliveries(apiKey: String): String {
        if (DemoData.isDemo(apiKey)) return DemoData.body()
        val body = try {
            Http.getText("$BASE/deliveries/?filter_mode=recent", mapOf("api-key" to apiKey))
        } catch (e: HttpException) {
            throw when (e.code) {
                429 -> ParcelException.RateLimit("HTTP 429")
                401, 403 -> ParcelException.Auth("HTTP ${e.code}")
                else -> failure(e.body) ?: ParcelException.Failure(e.message.orEmpty(), e)
            }
        }
        parseDeliveries(body)
        return body
    }

    fun parseDeliveries(body: String): List<Delivery> {
        val root = json(body)
        failure(root)?.let { throw it }
        return root.optJSONArray("deliveries").objects().mapNotNull(::delivery)
    }

    /** Carrier names by code. No key needed. */
    fun fetchCarriers(): String = Http.getText("$BASE/supported_carriers.json")

    /** Seen as {code: name} and {code: {"name": …}}; a list of {code, name} is accepted too. */
    fun parseCarriers(body: String): Map<String, String> {
        val trimmed = body.trimStart()
        val carriers = mutableMapOf<String, String>()
        try {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed).objects().forEach { item ->
                    val code = item.str("code")
                    if (code.isNotEmpty()) carriers[code] = item.str("name").ifEmpty { code }
                }
            } else {
                val root = JSONObject(trimmed)
                root.keys().forEach { code ->
                    val name = root.optJSONObject(code)?.str("name") ?: root.str(code)
                    carriers[code] = name.ifEmpty { code }
                }
            }
        } catch (e: JSONException) {
            throw ParcelException.Failure("Unexpected carriers list", e)
        }
        return carriers
    }

    private fun json(body: String): JSONObject = try {
        JSONObject(body)
    } catch (e: JSONException) {
        throw ParcelException.Failure("Invalid response from Parcel", e)
    }

    /** The error a {"success": false, "error_message": …} body stands for; null on success. */
    private fun failure(root: JSONObject): ParcelException? {
        if (root.optBoolean("success", false)) return null
        val message = root.str("error_message").ifEmpty { "Unknown Parcel error" }
        val lower = message.lowercase()
        return when {
            "api key" in lower || "api-key" in lower || "unauthorized" in lower -> ParcelException.Auth(message)
            "limit" in lower -> ParcelException.RateLimit(message)
            else -> ParcelException.Failure(message)
        }
    }

    private fun failure(body: String): ParcelException? = try {
        failure(JSONObject(body))
    } catch (e: JSONException) {
        null
    }

    private fun delivery(o: JSONObject): Delivery? {
        val tracking = o.str("tracking_number")
        if (tracking.isEmpty()) return null
        return Delivery(
            carrierCode = o.str("carrier_code"),
            trackingNumber = tracking,
            description = o.str("description"),
            status = Status.of(if (o.isNull("status_code")) null else o.optInt("status_code", Status.UNKNOWN.code)),
            events = o.optJSONArray("events").objects().map(::event),
            extraInformation = o.str("extra_information"),
            expectedFrom = o.time("timestamp_expected"),
            expectedTo = o.time("timestamp_expected_end"),
            dateExpected = o.date("date_expected"),
            dateExpectedEnd = o.date("date_expected_end")
        )
    }

    private fun event(o: JSONObject) = TrackingEvent(
        text = o.str("event"),
        date = o.str("date"),
        location = o.str("location"),
        additional = o.str("additional")
    )

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)

    private fun JSONObject.str(name: String): String = if (isNull(name)) "" else optString(name).trim()

    private fun JSONObject.time(name: String): Long? = if (isNull(name)) null else optLong(name).takeIf { it > 0 }

    // Parcel sends placeholders such as "--//--" when the carrier gave no date.
    private fun JSONObject.date(name: String): String? = str(name).takeIf { s -> s.any(Char::isDigit) && !s.startsWith("--") }
}
