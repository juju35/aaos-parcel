package me.jstephan.parcel.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Made-up deliveries for the API key "demo", with no network: the emulator run, the screenshots and the Google
 * Play review can show the app without a Parcel Premium account. Dates follow the clock, and the DHL parcel gets
 * a new scan every hour, so the hourly check also posts a notification.
 */
internal object DemoData {
    const val KEY = "demo"

    private const val HOUR = 3_600L
    private const val DAY = 24 * HOUR

    fun isDemo(apiKey: String): Boolean = apiKey.trim().equals(KEY, ignoreCase = true)

    fun body(nowMs: Long = System.currentTimeMillis()): String {
        val now = nowMs / 1000
        val deliveries = JSONArray()
            .put(headphones(now))
            .put(shoes(now))
            .put(books(now))
            .put(ink(now))
            .put(cable(now))
        return JSONObject().put("success", true).put("deliveries", deliveries).toString()
    }

    private fun today(now: Long) = now - now % DAY

    private fun headphones(now: Long) = delivery(
        description = "Casque audio",
        carrier = "lp",
        tracking = "6A23456789012",
        status = Status.OUT_FOR_DELIVERY,
        events = listOf(
            event("Votre colis est en cours de livraison", now - 2 * HOUR, "Rennes"),
            event("Votre colis est arrivé sur le site de distribution", now - 9 * HOUR, "Rennes PPDC"),
            event("Votre colis est pris en charge par La Poste", now - DAY - 5 * HOUR, "Paris")
        ),
        expected = today(now) to null
    )

    private fun shoes(now: Long) = delivery(
        description = "Chaussures de randonnée",
        carrier = "relay",
        tracking = "48291736",
        status = Status.AWAITING_PICKUP,
        events = listOf(
            event("Colis disponible en Point Relais", now - 5 * HOUR, "Le Kiosque, 12 rue de Paris, 35000 Rennes"),
            event("Colis en cours d'acheminement", now - DAY, "Agence de Rennes")
        ),
        extra = "À retirer avant le ${day(now + 8 * DAY)}, avec une pièce d'identité"
    )

    // A new scan every hour: its date changes the delivery's signature, so each hourly check notifies it.
    private fun books(now: Long) = delivery(
        description = "Livres",
        carrier = "dhl",
        tracking = "JD014600006281234567",
        status = Status.IN_TRANSIT,
        events = listOf(
            event("Scanné au centre de tri", now - now % HOUR, "Roissy-en-France"),
            event("Arrivé au pays de destination", now - DAY, "Paris"),
            event("Expédié", now - 3 * DAY, "Leipzig, Allemagne")
        ),
        expected = today(now) + 2 * DAY to today(now) + 3 * DAY
    )

    private fun ink(now: Long) = delivery(
        description = "Cartouches d'encre",
        carrier = "ups",
        tracking = "1Z999AA10123456784",
        status = Status.INFO_RECEIVED,
        events = listOf(event("Informations d'expédition reçues", now - 3 * HOUR, ""))
    )

    private fun cable(now: Long) = delivery(
        description = "Câble USB-C",
        carrier = "gls",
        tracking = "ZK41D9PQ",
        status = Status.DELIVERED,
        events = listOf(
            event("Colis livré", now - DAY - 4 * HOUR, "Rennes"),
            event("En cours de livraison", now - DAY - 7 * HOUR, "Rennes")
        )
    )

    private fun delivery(
        description: String,
        carrier: String,
        tracking: String,
        status: Status,
        events: List<JSONObject>,
        expected: Pair<Long, Long?>? = null,
        extra: String = ""
    ): JSONObject = JSONObject()
        .put("description", description)
        .put("carrier_code", carrier)
        .put("tracking_number", tracking)
        .put("status_code", status.code)
        .put("events", JSONArray(events))
        .put("extra_information", extra)
        .put("timestamp_expected", expected?.first ?: JSONObject.NULL)
        .put("timestamp_expected_end", expected?.second ?: JSONObject.NULL)

    private fun event(text: String, epochSeconds: Long, location: String): JSONObject = JSONObject()
        .put("event", text)
        .put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(epochSeconds * 1000)))
        .put("location", location)
        .put("additional", "")

    private fun day(epochSeconds: Long): String = SimpleDateFormat("d MMMM", Locale.FRANCE).format(Date(epochSeconds * 1000))
}
