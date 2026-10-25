package me.jstephan.parcel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelApiTest {

    private fun body(deliveries: String) = """{"success": true, "deliveries": [$deliveries]}"""

    @Test
    fun `parses a delivery`() {
        val list = ParcelApi.parseDeliveries(
            body(
                """
                {
                  "carrier_code": "lp", "tracking_number": " 6A123 ", "description": "Casque",
                  "status_code": 4, "extra_information": "Signature",
                  "timestamp_expected": 1761400000, "timestamp_expected_end": null,
                  "date_expected": "2026-10-26 09:00:00", "date_expected_end": "--//--",
                  "events": [
                    {"event": "En cours de livraison", "date": "26/10/2026 08:00", "location": "Rennes", "additional": null},
                    {"event": "Pris en charge", "date": "25/10/2026 18:00", "location": "Paris"}
                  ]
                }
                """
            )
        )
        val d = list.single()
        assertEquals("lp:6A123", d.key)
        assertEquals("Casque", d.title)
        assertEquals(Status.OUT_FOR_DELIVERY, d.status)
        assertEquals(1761400000L, d.expectedFrom)
        assertNull(d.expectedTo)
        assertEquals("2026-10-26 09:00:00", d.dateExpected)
        // Parcel's placeholder when the carrier gave no date.
        assertNull(d.dateExpectedEnd)
        assertEquals("En cours de livraison", d.lastEvent?.text)
        assertEquals("", d.lastEvent?.additional)
        assertEquals(2, d.events.size)
    }

    @Test
    fun `skips deliveries without a tracking number and maps unknown statuses`() {
        val list = ParcelApi.parseDeliveries(
            body("""{"tracking_number": ""}, {"tracking_number": "A", "status_code": 42}, {"tracking_number": "B", "status_code": null}""")
        )
        assertEquals(listOf("A", "B"), list.map { it.trackingNumber })
        assertTrue(list.all { it.status == Status.UNKNOWN })
        // No description: the tracking number is the title.
        assertEquals("A", list[0].title)
    }

    @Test
    fun `no deliveries is an empty list`() {
        assertEquals(emptyList<Delivery>(), ParcelApi.parseDeliveries("""{"success": true}"""))
    }

    @Test(expected = ParcelException.Auth::class)
    fun `a refused key is an auth error`() {
        ParcelApi.parseDeliveries("""{"success": false, "error_message": "Invalid API key"}""")
    }

    @Test(expected = ParcelException.RateLimit::class)
    fun `the hourly limit is a rate limit error`() {
        ParcelApi.parseDeliveries("""{"success": false, "error_message": "Rate limit exceeded"}""")
    }

    @Test(expected = ParcelException.Failure::class)
    fun `other errors are failures`() {
        ParcelApi.parseDeliveries("""{"success": false, "error_message": "Server error"}""")
    }

    @Test(expected = ParcelException.Failure::class)
    fun `a body that isn't JSON is a failure`() {
        ParcelApi.parseDeliveries("<html>502</html>")
    }

    @Test
    fun `parses the carriers in each shape Parcel used`() {
        val expected = mapOf("lp" to "La Poste", "dhl" to "DHL Express")
        assertEquals(expected, ParcelApi.parseCarriers("""{"lp": "La Poste", "dhl": "DHL Express"}"""))
        assertEquals(expected, ParcelApi.parseCarriers("""{"lp": {"name": "La Poste"}, "dhl": {"name": "DHL Express"}}"""))
        assertEquals(
            expected,
            ParcelApi.parseCarriers("""[{"code": "lp", "name": "La Poste"}, {"code": "dhl", "name": "DHL Express"}, {"name": "?"}]""")
        )
        // No name: the code.
        assertEquals(mapOf("gls" to "gls"), ParcelApi.parseCarriers("""{"gls": {"name": ""}}"""))
    }

    @Test
    fun `the demo key gives deliveries without network`() {
        val list = ParcelApi.parseDeliveries(ParcelApi.fetchDeliveries(" Demo "))
        assertEquals(5, list.size)
        assertTrue(list.any { it.status == Status.AWAITING_PICKUP && it.lastEvent?.location?.isNotEmpty() == true })
    }
}
