/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordExtractionTest {

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is", "are", "was", "were", "of")
        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && it !in stopWords }
    }

    @Test
    fun testExtraction() {
        val title = "The Future of Quantum Computing and AI in 2026"
        val keywords = extractKeywords(title)
        
        assertTrue(keywords.contains("future"))
        assertTrue(keywords.contains("quantum"))
        assertTrue(keywords.contains("computing"))
        assertTrue(keywords.contains("2026"))
        
        // Check stop words removed
        assertTrue(!keywords.contains("the"))
        assertTrue(!keywords.contains("of"))
        assertTrue(!keywords.contains("and"))
        
        // Check short words filtered
        assertTrue(!keywords.contains("ai")) // length 2
    }
}
