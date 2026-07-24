/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

import org.schabi.newpipe.extractor.Page

data class PaginatedList<T>(
    val items: List<T>,
    val nextPage: Page?
)
