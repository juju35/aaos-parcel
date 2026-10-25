package me.jstephan.parcel.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class DatesTest {

    @Test
    fun `formats Parcel's date texts as a day`() {
        assertEquals("lun. 26 oct.", Dates.day("2026-10-26 09:00:00"))
        assertEquals("lun. 26 oct.", Dates.day("2026-10-26T09:00:00"))
        assertEquals("lun. 26 oct.", Dates.day("2026-10-26 09:00"))
        assertEquals("lun. 26 oct.", Dates.day("2026-10-26"))
        assertEquals("lun. 26 oct.", Dates.day("26/10/2026"))
    }

    @Test
    fun `keeps a text it can't read`() {
        assertEquals("Fin de semaine", Dates.day("Fin de semaine"))
        assertNull(Dates.parse("2026-13-45"))
        assertNull(Dates.parse("2026-10-26 nope"))
    }

    @Test
    fun `formats timestamps in the car's time zone`() {
        val zone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"))
            // 2026-10-25 23:30 UTC is already Monday 26 in Paris.
            assertEquals("lun. 26 oct.", Dates.day(1_792_971_000L))
        } finally {
            TimeZone.setDefault(zone)
        }
    }
}
