/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import android.util.Base64
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.utils.PTLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveDashManifestGenerator @Inject constructor() {

    /**
     * Generates a lightweight, standards-compliant MPEG-DASH MPD XML string directly
     * from the extracted adaptive video & audio formats.
     */
    fun generateMpdXml(bundle: StreamBundle, durationSeconds: Long = 0L): String {
        val durationAttr = if (durationSeconds > 0) "PT${durationSeconds}S" else "PT3600S"
        val videoStreams = bundle.videoStreams
        val audioStreams = bundle.audioStreams

        val xmlBuilder = StringBuilder()
        xmlBuilder.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        xmlBuilder.append(
            """<MPD xmlns="urn:mpeg:dash:schema:mpd:2011" profiles="urn:mpeg:dash:profile:isoff-on-demand:2011" type="static" minBufferTime="PT1.5S" mediaPresentationDuration="$durationAttr">"""
        ).append("\n")
        xmlBuilder.append("""  <Period id="0" start="PT0S">""").append("\n")

        // 1. Video AdaptationSet
        if (videoStreams.isNotEmpty()) {
            val videoMime = if (videoStreams.any { it.format.contains("webm") }) "video/webm" else "video/mp4"
            xmlBuilder.append("""    <AdaptationSet mimeType="$videoMime" subsegmentAlignment="true">""").append("\n")

            for ((index, stream) in videoStreams.withIndex()) {
                val resDigits = stream.quality.filter { it.isDigit() }.toIntOrNull() ?: 720
                val (width, height) = parseDimensions(resDigits)
                val bandwidth = estimateVideoBitrate(resDigits)
                val codec = if (stream.format.contains("webm")) "vp9" else "avc1.640028"
                val escapedUrl = escapeXml(stream.url)

                xmlBuilder.append(
                    """      <Representation id="v_$index" bandwidth="$bandwidth" width="$width" height="$height" codecs="$codec">"""
                ).append("\n")
                xmlBuilder.append("""        <BaseURL>$escapedUrl</BaseURL>""").append("\n")
                xmlBuilder.append("""      </Representation>""").append("\n")
            }
            xmlBuilder.append("""    </AdaptationSet>""").append("\n")
        }

        // 2. Audio AdaptationSet
        if (audioStreams.isNotEmpty()) {
            val audioMime = if (audioStreams.any { it.format.contains("webm") || it.format.contains("opus") }) "audio/webm" else "audio/mp4"
            xmlBuilder.append("""    <AdaptationSet mimeType="$audioMime" subsegmentAlignment="true">""").append("\n")

            for ((index, stream) in audioStreams.withIndex()) {
                val bitrateKbps = stream.quality.filter { it.isDigit() }.toIntOrNull() ?: 128
                val bandwidth = bitrateKbps * 1000
                val codec = if (stream.format.contains("webm") || stream.format.contains("opus")) "opus" else "mp4a.40.2"
                val escapedUrl = escapeXml(stream.url)

                xmlBuilder.append(
                    """      <Representation id="a_$index" bandwidth="$bandwidth" codecs="$codec">"""
                ).append("\n")
                xmlBuilder.append("""        <BaseURL>$escapedUrl</BaseURL>""").append("\n")
                xmlBuilder.append("""      </Representation>""").append("\n")
            }
            xmlBuilder.append("""    </AdaptationSet>""").append("\n")
        }

        xmlBuilder.append("""  </Period>""").append("\n")
        xmlBuilder.append("""</MPD>""")

        return xmlBuilder.toString()
    }

    /**
     * Converts MPD XML into an in-memory Data URI string suitable for Media3 parsing.
     */
    fun generateDataUri(bundle: StreamBundle, durationSeconds: Long = 0L): String {
        val xml = generateMpdXml(bundle, durationSeconds)
        val base64Xml = Base64.encodeToString(xml.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        PTLog.d("AdaptiveDashManifestGenerator", "Generated synthetic DASH MPD Data URI (xml length: ${xml.length})")
        return "data:application/dash+xml;base64,$base64Xml"
    }

    private fun parseDimensions(height: Int): Pair<Int, Int> {
        return when {
            height >= 2160 -> Pair(3840, 2160)
            height >= 1440 -> Pair(2560, 1440)
            height >= 1080 -> Pair(1920, 1080)
            height >= 720 -> Pair(1280, 720)
            height >= 480 -> Pair(854, 480)
            height >= 360 -> Pair(640, 360)
            else -> Pair(426, 240)
        }
    }

    private fun estimateVideoBitrate(height: Int): Int {
        return when {
            height >= 2160 -> 15_000_000
            height >= 1440 -> 8_000_000
            height >= 1080 -> 4_000_000
            height >= 720 -> 2_000_000
            height >= 480 -> 1_000_000
            else -> 500_000
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
