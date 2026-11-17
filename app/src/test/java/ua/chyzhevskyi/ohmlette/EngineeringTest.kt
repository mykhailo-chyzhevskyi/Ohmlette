package ua.chyzhevskyi.ohmlette

import org.junit.Assert.*
import org.junit.Test

class EngineeringTest {
    @Test fun prefixesAreCaseSensitive() {
        assertEquals(0.001, Engineering.parse("1m"), 1e-12)
        assertEquals(1e6, Engineering.parse("1M"), 0.0)
        assertEquals(4700.0, Engineering.parse("4,7k"), 1e-9)
    }
    @Test fun megaToPico() { assertEquals(1e18, Engineering.convert("1", 6, -12), 0.0) }
    @Test fun crossesDecadeBoundary() { assertEquals(10.0, Engineering.nearest(9.9, 12).first().resistance, 1e-9) }
    @Test fun canonicalE96Nominal() { assertEquals(4990.0, Engineering.nearest(4990.0, 96).first().resistance, 1e-9) }
    @Test fun rejectsInvalidResistance() { assertTrue(runCatching { Engineering.nearest(-1.0, 24) }.isFailure) }
    @Test fun rejectsOverflow() { assertTrue(runCatching { Engineering.parse("1e999") }.isFailure) }
}
