package me.jstephan.parcel.ui

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Expected delivery days, as "sam. 25 oct.". */
object Dates {
    // What Parcel's date_expected has been seen as.
    private val PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "dd/MM/yyyy")

    fun day(epochSeconds: Long): String = format(Date(epochSeconds * 1000))

    /** [text] as a day when it is one of Parcel's date formats, else as Parcel wrote it. */
    fun day(text: String): String = parse(text)?.let(::format) ?: text

    /** The date in [text], which must match a whole pattern (no time left over, no lenient rollover). */
    fun parse(text: String): Date? {
        val trimmed = text.trim()
        return PATTERNS.firstNotNullOfOrNull { pattern ->
            val position = ParsePosition(0)
            SimpleDateFormat(pattern, Locale.ROOT).apply { isLenient = false }
                .parse(trimmed, position)
                ?.takeIf { position.index == trimmed.length }
        }
    }

    private fun format(date: Date): String = SimpleDateFormat("EEE d MMM", Locale.FRANCE).format(date)
}
