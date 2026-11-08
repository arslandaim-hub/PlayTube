/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import org.junit.Test
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class ShortsDetectionTest {
    @Test
    fun testShort() {
        val methods = StreamInfoItem::class.java.methods
        methods.forEach {
            if (it.name.startsWith("get") || it.name.startsWith("is")) {
                println(it.name)
            }
        }
    }
}
