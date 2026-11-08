/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.recommendation.NeuroTokenizer
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordExtractionTest {

    @Test
    fun testExtraction() {
        val title = "The Future of Quantum Computing and AI in 4k"
        val tokens = NeuroTokenizer.tokenize(title)
        
        assertTrue(tokens.containsKey("future"))
        assertTrue(tokens.containsKey("quantum"))
        assertTrue(tokens.containsKey("computing"))
        
        // High-signal 2-char tokens retained
        assertTrue(tokens.containsKey("ai"))
        assertTrue(tokens.containsKey("4k"))
        
        // Stop words removed
        assertTrue(!tokens.containsKey("the"))
        assertTrue(!tokens.containsKey("of"))
        assertTrue(!tokens.containsKey("and"))
        assertTrue(!tokens.containsKey("in"))
    }
}
