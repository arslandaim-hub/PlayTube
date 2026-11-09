/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.SubtitleItem
import com.arslandaim.playtube.utils.CaptionTrackResolver
import javax.inject.Inject

@UnstableApi
class PlayerTrackManager @Inject constructor() {

    fun updateCcState(
        player: Player,
        enabled: Boolean,
        selectedSubtitle: SubtitleItem? = null,
        preferredLang: String? = null
    ) {
        val parametersBuilder = player.trackSelectionParameters.buildUpon()

        if (enabled) {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
            parametersBuilder.setSelectUndeterminedTextLanguage(true)

            val lang = selectedSubtitle?.languageTag ?: preferredLang
            val targetId = selectedSubtitle?.id

            if (!lang.isNullOrBlank()) {
                parametersBuilder.setPreferredTextLanguage(lang)
            }

            val currentTracks = player.currentTracks
            val textGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }

            var selectedOverride: TrackSelectionOverride? = null

            // 1. First try matching exact group by format.id (e.g. "en:asr" or "en:manual")
            // Note: MergingMediaSource prefixes child track IDs with "<sourceIndex>:" (e.g. "1:en:asr")
            if (!targetId.isNullOrBlank()) {
                for (group in textGroups) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val formatId = format.id ?: ""
                        if (formatId.equals(targetId, ignoreCase = true) ||
                            formatId.endsWith(":$targetId", ignoreCase = true)
                        ) {
                            selectedOverride = TrackSelectionOverride(group.mediaTrackGroup, i)
                            break
                        }
                    }
                    if (selectedOverride != null) break
                }
            }

            // 2. Try matching language tag if no ID match found (prefer exact match & manual tracks first)
            if (selectedOverride == null && !lang.isNullOrBlank()) {
                val candidates = mutableListOf<Pair<TrackSelectionOverride, Int>>()
                for (group in textGroups) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val trackLang = format.language ?: ""
                        val trackLabel = format.label ?: ""
                        val formatId = format.id ?: ""
                        val isManual = formatId.endsWith(":manual", ignoreCase = true)
                        val score = when {
                            trackLang.equals(lang, ignoreCase = true) && isManual -> 4
                            trackLang.equals(lang, ignoreCase = true) -> 3
                            (trackLang.startsWith(lang, ignoreCase = true) || lang.startsWith(trackLang, ignoreCase = true)) && trackLang.isNotBlank() -> 2
                            trackLabel.contains(lang, ignoreCase = true) -> 1
                            else -> 0
                        }
                        if (score > 0) {
                            candidates.add(TrackSelectionOverride(group.mediaTrackGroup, i) to score)
                        }
                    }
                }
                selectedOverride = candidates.maxByOrNull { it.second }?.first
            }

            // 3. Fallback: prefer English track if available, otherwise first supported text track
            if (selectedOverride == null && textGroups.isNotEmpty()) {
                for (group in textGroups) {
                    for (i in 0 until group.length) {
                        val trackLang = group.getTrackFormat(i).language ?: ""
                        if (trackLang.startsWith("en", ignoreCase = true)) {
                            selectedOverride = TrackSelectionOverride(group.mediaTrackGroup, i)
                            break
                        }
                    }
                    if (selectedOverride != null) break
                }
                if (selectedOverride == null) {
                    selectedOverride = TrackSelectionOverride(textGroups.first().mediaTrackGroup, 0)
                }
            }

            if (selectedOverride != null) {
                parametersBuilder.addOverride(selectedOverride)
            }
        } else {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        }

        player.trackSelectionParameters = parametersBuilder.build()
    }

    fun updateCcState(player: Player, enabled: Boolean, preferredLang: String?) {
        updateCcState(player = player, enabled = enabled, selectedSubtitle = null, preferredLang = preferredLang)
    }

    fun createSubtitleConfigs(
        bundle: StreamBundle,
        selectedSubtitle: SubtitleItem? = null,
        preferredLang: String? = null
    ): List<MediaItem.SubtitleConfiguration> {
        val localTracks = bundle.subtitles.filter {
            it.url.isNotBlank() && (it.url.startsWith("file:") || it.url.startsWith("content:"))
        }
        val tracksToAttach = if (localTracks.isNotEmpty()) {
            localTracks
        } else {
            // Fallback: attach matching remote subtitle track (or English/first track) if local cache wasn't created
            val targetLang = selectedSubtitle?.languageTag ?: preferredLang
            val matchedRemote = bundle.subtitles.find { sub ->
                sub.url.isNotBlank() && (
                    (selectedSubtitle != null && sub.id == selectedSubtitle.id) ||
                    (!targetLang.isNullOrBlank() && sub.languageTag.equals(targetLang, ignoreCase = true))
                )
            } ?: bundle.subtitles.find { it.url.isNotBlank() && it.languageTag.startsWith("en", ignoreCase = true) }
              ?: bundle.subtitles.firstOrNull { it.url.isNotBlank() }

            listOfNotNull(matchedRemote)
        }

        return tracksToAttach.map { subtitle ->
            val (normalizedUrl, mimeType) = CaptionTrackResolver.normalizeSubtitleUrl(subtitle.url, subtitle.format)
            val trackId = if (subtitle.id.isNotBlank()) subtitle.id else "${subtitle.languageTag}:${if (subtitle.isAutoGenerated) "asr" else "manual"}"
            val label = CaptionTrackResolver.buildDisplayLabel(subtitle.languageTag, subtitle.isAutoGenerated, subtitle.name)

            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(normalizedUrl))
                .setId(trackId)
                .setMimeType(mimeType)
                .setLanguage(subtitle.languageTag)
                .setLabel(label)
                .setRoleFlags(C.ROLE_FLAG_CAPTION or C.ROLE_FLAG_SUBTITLE)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
    }
}
