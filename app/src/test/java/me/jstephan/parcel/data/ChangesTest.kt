package me.jstephan.parcel.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangesTest {

    private fun delivery(tracking: String, status: Status, vararg events: Pair<String, String>) = Delivery(
        carrierCode = "lp",
        trackingNumber = tracking,
        description = tracking,
        status = status,
        events = events.map { (text, date) -> TrackingEvent(text, date, "Rennes", "") },
        extraInformation = "",
        expectedFrom = null,
        expectedTo = null,
        dateExpected = null,
        dateExpectedEnd = null
    )

    private val transit = delivery("A", Status.IN_TRANSIT, "Expédié" to "24/10")
    private val pickup = delivery("B", Status.AWAITING_PICKUP, "Disponible" to "24/10")

    @Test
    fun `nothing is news on the first check`() {
        assertEquals(emptyList<Delivery>(), Changes.detect(null, listOf(transit, pickup)))
    }

    @Test
    fun `nothing changed`() {
        val seen = Changes.signatures(listOf(transit, pickup))
        assertEquals(emptyList<Delivery>(), Changes.detect(seen, listOf(transit, pickup)))
    }

    @Test
    fun `a new status or a new latest event is news`() {
        val seen = Changes.signatures(listOf(transit, pickup))
        val delivered = delivery("A", Status.DELIVERED, "Livré" to "25/10", "Expédié" to "24/10")
        val rescanned = delivery("B", Status.AWAITING_PICKUP, "Disponible" to "25/10")
        assertEquals(listOf(delivered, rescanned), Changes.detect(seen, listOf(delivered, rescanned)))
    }

    @Test
    fun `a delivery added since the last check is news`() {
        val seen = Changes.signatures(listOf(transit))
        assertEquals(listOf(pickup), Changes.detect(seen, listOf(transit, pickup)))
    }

    @Test
    fun `older events trimmed by Parcel are not news`() {
        val long = delivery("A", Status.IN_TRANSIT, "Tri" to "25/10", "Expédié" to "24/10")
        val trimmed = delivery("A", Status.IN_TRANSIT, "Tri" to "25/10")
        assertEquals(emptyList<Delivery>(), Changes.detect(Changes.signatures(listOf(long)), listOf(trimmed)))
    }
}
