/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import java.util.regex.Pattern

object SubtitleSanitizer {

    private val INLINE_TIMESTAMP_REGEX = Pattern.compile("<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>")
    private val HTML_TAG_REGEX = Pattern.compile("<[^>]+>")
    private val WEBVTT_HEADER_REGEX = Pattern.compile("^WEBVTT.*$", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE)
    private val CUE_TIMING_REGEX = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}|\\d{2}:\\d{2}\\.\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}|\\d{2}:\\d{2}\\.\\d{3})(.*)$")

    /**
     * Cleans an individual cue text string by removing inline voice, class, timestamp, and XML/HTML markup tags.
     */
    fun cleanCueText(rawText: String): String {
        if (rawText.isBlank()) return ""
        val unescaped = rawText.replace('\u00A0', ' ')
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        val noTimestamps = INLINE_TIMESTAMP_REGEX.matcher(unescaped).replaceAll("")
        val noTags = HTML_TAG_REGEX.matcher(noTimestamps).replaceAll("")

        val lines = noTags.lines()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return ""

        // Join word lines with space to prevent single-word vertical stacking
        return lines.joinToString(" ").trim()
    }

    /**
     * Sanitizes full WebVTT string content (e.g. from YouTube auto-generated captions).
     * Consolidates rapid word-by-word ASR fragments into clean, single-line/2-line sentence blocks.
     */
    fun sanitizeWebVtt(vttContent: String): String {
        if (vttContent.isBlank()) return "WEBVTT\n\n"

        val lines = vttContent.lines()
        val rawCues = mutableListOf<RawCue>()

        var currentTiming: String? = null
        var currentStartMs = 0L
        var currentEndMs = 0L
        var currentTextLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            val matcher = CUE_TIMING_REGEX.matcher(trimmed)

            if (matcher.matches()) {
                // Save previous cue block if present
                if (currentTiming != null && currentTextLines.isNotEmpty()) {
                    val cleanText = cleanCueText(currentTextLines.joinToString(" "))
                    if (cleanText.isNotBlank()) {
                        rawCues.add(RawCue(currentStartMs, currentEndMs, cleanText))
                    }
                }

                val startStr = matcher.group(1) ?: "00:00.000"
                val endStr = matcher.group(2) ?: "00:00.000"
                currentStartMs = parseTimestampToMs(startStr)
                currentEndMs = parseTimestampToMs(endStr)
                currentTiming = trimmed
                currentTextLines = mutableListOf()
            } else if (trimmed.isNotBlank() && !WEBVTT_HEADER_REGEX.matcher(trimmed).find() && !trimmed.startsWith("NOTE") && !trimmed.all { it.isDigit() }) {
                currentTextLines.add(trimmed)
            }
        }

        // Add last block
        if (currentTiming != null && currentTextLines.isNotEmpty()) {
            val cleanText = cleanCueText(currentTextLines.joinToString(" "))
            if (cleanText.isNotBlank()) {
                rawCues.add(RawCue(currentStartMs, currentEndMs, cleanText))
            }
        }

        if (rawCues.isEmpty()) return "WEBVTT\n\n"

        // Consolidate rapid word/chunk ASR fragment cues into stable single-line/2-line sentence blocks
        val consolidated = mutableListOf<RawCue>()

        for (cue in rawCues) {
            if (consolidated.isEmpty()) {
                consolidated.add(cue)
            } else {
                val lastIndex = consolidated.size - 1
                val last = consolidated[lastIndex]

                val isExactDuplicate = last.text.equals(cue.text, ignoreCase = true)
                val isPrefix = cue.text.startsWith(last.text, ignoreCase = true)
                val isSuffix = last.text.endsWith(cue.text, ignoreCase = true)
                val isOverlappingOrClose = cue.startMs <= last.endMs + 1200L

                if (isExactDuplicate || isPrefix) {
                    // Cumulative prefix update (YouTube progressive building cue)
                    val newEnd = maxOf(last.endMs, cue.endMs, last.startMs + 2000L)
                    consolidated[lastIndex] = RawCue(last.startMs, newEnd, cue.text)
                } else if (isSuffix) {
                    // Sub-fragment duplicate
                    val newEnd = maxOf(last.endMs, cue.endMs, last.startMs + 2000L)
                    consolidated[lastIndex] = RawCue(last.startMs, newEnd, last.text)
                } else if (isOverlappingOrClose && !hasSentenceEnding(last.text)) {
                    // Combine into continuous horizontal line if length permits
                    val combinedText = "${last.text} ${cue.text}".replace(Regex("\\s+"), " ").trim()

                    if (combinedText.length <= 80) {
                        val newEnd = maxOf(last.endMs, cue.endMs, last.startMs + 2000L)
                        consolidated[lastIndex] = RawCue(last.startMs, newEnd, combinedText)
                    } else {
                        // Sentence line limit reached, cap previous cue duration and start new cue block
                        val adjustedPrevEnd = maxOf(last.endMs, last.startMs + 2000L).coerceAtMost(cue.startMs)
                        consolidated[lastIndex] = RawCue(last.startMs, adjustedPrevEnd, last.text)
                        consolidated.add(cue)
                    }
                } else {
                    // Natural sentence boundary or time gap > 1.2s
                    val adjustedPrevEnd = maxOf(last.endMs, last.startMs + 2000L).coerceAtMost(cue.startMs)
                    consolidated[lastIndex] = RawCue(last.startMs, adjustedPrevEnd, last.text)
                    consolidated.add(cue)
                }
            }
        }

        // Format consolidated cues into clean 1-line or 2-line WebVTT entries
        val sb = StringBuilder("WEBVTT\n\n")
        for ((index, cue) in consolidated.withIndex()) {
            val startTs = formatMsToTimestamp(cue.startMs)
            val minDisplayDuration = if (cue.text.length > 40) 3000L else 2000L
            val endTs = formatMsToTimestamp(maxOf(cue.endMs, cue.startMs + minDisplayDuration))

            val formattedText = formatLineBreaksIfNeeded(cue.text)

            sb.append(index + 1).append("\n")
            sb.append(startTs).append(" --> ").append(endTs).append("\n")
            sb.append(formattedText).append("\n\n")
        }

        return sb.toString()
    }

    private fun hasSentenceEnding(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith("!")
    }

    /**
     * Formats long cue text into at most 2 balanced horizontal lines.
     * Prevents single-word line breaks.
     */
    private fun formatLineBreaksIfNeeded(text: String): String {
        val clean = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 42) return clean

        val words = clean.split(" ")
        if (words.size <= 3) return clean

        val targetLen = clean.length / 2
        var currentLen = 0
        var splitIndex = -1

        for (i in words.indices) {
            currentLen += words[i].length + 1
            if (currentLen >= targetLen) {
                splitIndex = i
                break
            }
        }

        if (splitIndex in 1 until words.size - 1) {
            val line1 = words.subList(0, splitIndex + 1).joinToString(" ")
            val line2 = words.subList(splitIndex + 1, words.size).joinToString(" ")
            return "$line1\n$line2"
        }

        return clean
    }

    private data class RawCue(
        val startMs: Long,
        val endMs: Long,
        val text: String
    )

    private fun parseTimestampToMs(ts: String): Long {
        return try {
            val parts = ts.trim().split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secMs = parts[2].split(".")
                val seconds = secMs[0].toLong()
                val ms = secMs.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000 + ms
            } else if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val secMs = parts[1].split(".")
                val seconds = secMs[0].toLong()
                val ms = secMs.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                (minutes * 60 + seconds) * 1000 + ms
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatMsToTimestamp(ms: Long): String {
        val totalSec = ms / 1000
        val millis = ms % 1000
        val seconds = totalSec % 60
        val totalMin = totalSec / 60
        val minutes = totalMin % 60
        val hours = totalMin / 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}
