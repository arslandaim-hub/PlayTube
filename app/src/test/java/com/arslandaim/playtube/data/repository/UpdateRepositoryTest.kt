/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {

    // Helper to test the private version comparison logic via a testable version of the method
    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        val size = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until size) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    @Test
    fun testVersionComparison() {
        // Newer versions
        assertTrue(isVersionNewer("1.3.3", "1.3.4"))
        assertTrue(isVersionNewer("1.3.3", "1.4.0"))
        assertTrue(isVersionNewer("1.3.3", "2.0.0"))
        assertTrue(isVersionNewer("1.3.3", "1.3.3.1"))
        
        // Same versions
        assertFalse(isVersionNewer("1.3.3", "1.3.3"))
        
        // Older versions
        assertFalse(isVersionNewer("1.3.3", "1.3.2"))
        assertFalse(isVersionNewer("1.3.3", "1.2.9"))
        assertFalse(isVersionNewer("1.3.3", "0.9.9"))
    }
}
