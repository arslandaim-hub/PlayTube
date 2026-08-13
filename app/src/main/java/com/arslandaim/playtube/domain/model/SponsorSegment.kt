/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SponsorSegment(
    val category: String,
    val segment: List<Float>, // [start, end]
    val UUID: String
) {
    val startMs: Long get() = (segment.getOrNull(0) ?: 0f).times(1000).toLong()
    val endMs: Long get() = (segment.getOrNull(1) ?: 0f).times(1000).toLong()
}
