package me.jstephan.parcel.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.jstephan.parcel.Prefs
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** A check's result: every delivery, and those Parcel has news about since the previous check. */
class Update(val deliveries: List<Delivery>, val changed: List<Delivery>)

/**
 * The deliveries of the last check, shared by the screens and the hourly check. The last answer is kept on disk,
 * with each delivery's [Delivery.signature], to show the list straight away and to tell what changed next time.
 */
class DeliveryRepository private constructor(context: Context) {
    private val prefs = Prefs(context)
    private val deliveriesFile = File(context.filesDir, "deliveries.json")
    private val seenFile = File(context.filesDir, "seen.json")
    private val carriersFile = File(context.filesDir, "carriers.json")
    private val mutex = Mutex()

    private var loaded = false
    private var deliveries: List<Delivery>? = null
    private var carriers: Map<String, String> = emptyMap()

    /** Deliveries of the last check; null before the first one. */
    suspend fun cached(): List<Delivery>? = locked { deliveries }

    fun isStale(maxAgeMs: Long): Boolean = System.currentTimeMillis() - prefs.lastCheck > maxAgeMs

    fun carrierName(code: String): String = carriers[code] ?: code

    /** Asks Parcel for the deliveries. Nothing counts as changed on the very first check. */
    suspend fun refresh(): Update = locked {
        val key = prefs.apiKey ?: throw ParcelException.Auth("No API key")
        val update = store(ParcelApi.fetchDeliveries(key))
        refreshCarriers()
        update
    }

    /**
     * Starts over for a new key, from the deliveries [body] it was checked with (null if there isn't one): the
     * deliveries already there are not notified as news.
     */
    suspend fun reset(body: String?) {
        locked {
            deliveries = null
            prefs.lastCheck = 0L
            deliveriesFile.delete()
            seenFile.delete()
            body?.let {
                store(it)
                refreshCarriers()
            }
        }
    }

    private fun store(body: String): Update {
        val list = ParcelApi.parseDeliveries(body)
        val seen = readSeen()
        val changed = if (seen == null) emptyList() else list.filter { d -> seen[d.key].let { it != null && it != d.signature } }
        deliveriesFile.writeText(body)
        seenFile.writeText(JSONObject(list.associate { it.key to it.signature }).toString())
        deliveries = list
        prefs.lastCheck = System.currentTimeMillis()
        return Update(list, changed)
    }

    /** Signatures of the previous check; null when there was none. */
    private fun readSeen(): Map<String, String>? {
        if (!seenFile.exists()) return null
        return try {
            val root = JSONObject(seenFile.readText())
            root.keys().asSequence().associateWith { root.optString(it) }
        } catch (e: JSONException) {
            null
        }
    }

    /** Carrier names, downloaded again once a day; the old list stays on failure. */
    private fun refreshCarriers() {
        if (carriersFile.exists() && System.currentTimeMillis() - carriersFile.lastModified() < CARRIERS_MAX_AGE_MS) return
        try {
            val body = ParcelApi.fetchCarriers()
            carriers = ParcelApi.parseCarriers(body)
            carriersFile.writeText(body)
        } catch (e: IOException) {
            Log.w(TAG, "Could not load the carriers", e)
        }
    }

    private fun load() {
        if (loaded) return
        loaded = true
        deliveries = readOrNull(deliveriesFile, ParcelApi::parseDeliveries)
        carriers = readOrNull(carriersFile, ParcelApi::parseCarriers).orEmpty()
    }

    private fun <T> readOrNull(file: File, parse: (String) -> T): T? = try {
        if (file.exists()) parse(file.readText()) else null
    } catch (e: IOException) {
        Log.w(TAG, "Unreadable cache ${file.name}", e)
        null
    }

    private suspend fun <T> locked(block: () -> T): T = mutex.withLock {
        withContext(Dispatchers.IO) {
            load()
            block()
        }
    }

    companion object {
        private const val TAG = "DeliveryRepository"
        private const val CARRIERS_MAX_AGE_MS = 24 * 60 * 60 * 1000L

        @Volatile
        private var instance: DeliveryRepository? = null

        fun get(context: Context): DeliveryRepository = instance ?: synchronized(this) {
            instance ?: DeliveryRepository(context.applicationContext).also { instance = it }
        }
    }
}
