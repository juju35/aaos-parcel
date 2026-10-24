package me.jstephan.parcel.data

import me.jstephan.parcel.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpException(val code: Int, val body: String) : IOException("HTTP $code ${body.take(200)}")

/** Blocking HTTP GET: call it from Dispatchers.IO. */
internal object Http {
    private const val USER_AGENT = "Parcel-AAOS/${BuildConfig.VERSION_NAME} (+https://github.com/juju35/aaos-parcel)"

    fun getText(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 30_000): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw HttpException(code, conn.errorStream?.use { it.readBytes() }?.decodeToString().orEmpty())
            }
            return conn.inputStream.use { it.readBytes() }.decodeToString()
        } finally {
            conn.disconnect()
        }
    }
}
