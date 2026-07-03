/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

data class StreamItem(
    val url: String,
    val quality: String,
    val format: String,
    val size: Long = -1,
    val isAdaptive: Boolean = false,
    val languageTag: String? = null,
    val trackType: String? = null
)
