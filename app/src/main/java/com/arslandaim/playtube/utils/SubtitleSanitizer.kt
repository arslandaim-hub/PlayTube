/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import java.util.regex.Pattern

object SubtitleSanitizer {

    private val INLINE_TIMESTAMP_REGEX = Pattern.compile("<\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{3}>|<\\d{1,2}:\\d{2}[.,]\\d{3}>")
    private val HTML_TAG_REGEX = Pattern.compile("<[^>]+>")
    private val WEBVTT_HEADER_REGEX = Pattern.compile("^\\uFEFF?WEBVTT.*$", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE)
    private val CUE_TIMING_REGEX = Pattern.compile("^(\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{3}|\\d{1,2}:\\d{2}[.,]\\d{3})\\s*-->\\s*(\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{3}|\\d{1,2}:\\d{2}[.,]\\d{3})(.*)$")

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

    private const val MAX_SINGLE_LINE_CHARS = 42

    /**
     * Sanitizes full WebVTT string content (e.g. from YouTube auto-generated captions).
     * Consolidates rapid word-by-word ASR fragments and splits long cues into sequential
     * single-line cues so subtitles always render on one line.
     */
    fun sanitizeWebVtt(vttContent: String): String {
        val normalizedContent = vttContent.removePrefix("\uFEFF")
        if (normalizedContent.isBlank()) return "WEBVTT\n\n"

        val lines = normalizedContent.lines()
        val rawCues = mutableListOf<RawCue>()

        var currentTiming: String? = null
        var currentStartMs = 0L
        var currentEndMs = 0L
        var currentTextLines = mutableListOf<String>()

        fun flushCueBlock() {
            if (currentTiming == null || currentTextLines.isEmpty()) return
            val cleanedLines = currentTextLines
                .map { cleanCueText(it) }
                .filter { it.isNotBlank() }
            if (cleanedLines.isEmpty()) return

            // Remove YouTube ASR rollup duplicate lines that repeat the previous cue
            val lastCueText = rawCues.lastOrNull()?.text
            val deduplicatedLines = if (lastCueText != null && cleanedLines.size > 1) {
                cleanedLines.dropWhile { line ->
                    line.equals(lastCueText, ignoreCase = true) ||
                        lastCueText.endsWith(line, ignoreCase = true)
                }
            } else {
                cleanedLines
            }

            val finalText = deduplicatedLines.joinToString(" ").replace(Regex("\\s+"), " ").trim()
            if (finalText.isNotBlank() && currentEndMs > currentStartMs) {
                rawCues.add(RawCue(currentStartMs, currentEndMs, finalText))
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            val matcher = CUE_TIMING_REGEX.matcher(trimmed)

            if (matcher.matches()) {
                flushCueBlock()

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

        // Flush last block
        flushCueBlock()

        if (rawCues.isEmpty()) {
            rawCues.addAll(extractXmlOrTtmlCues(normalizedContent))
        }

        if (rawCues.isEmpty()) return "WEBVTT\n\n"

        // Consolidate rapid word/chunk ASR fragment cues into stable single-line sentence blocks
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
                val isOverlappingOrClose = cue.startMs <= last.endMs + 800L

                if (isExactDuplicate || isPrefix) {
                    // Cumulative prefix update (YouTube progressive building cue)
                    val newEnd = maxOf(last.endMs, cue.endMs)
                    consolidated[lastIndex] = RawCue(last.startMs, newEnd, cue.text)
                } else if (isSuffix) {
                    // Sub-fragment duplicate
                    val newEnd = maxOf(last.endMs, cue.endMs)
                    consolidated[lastIndex] = RawCue(last.startMs, newEnd, last.text)
                } else if (isOverlappingOrClose && !hasSentenceEnding(last.text)) {
                    // Combine into continuous single horizontal line only if it fits on one line
                    val combinedText = "${last.text} ${cue.text}".replace(Regex("\\s+"), " ").trim()

                    if (combinedText.length <= MAX_SINGLE_LINE_CHARS) {
                        val newEnd = maxOf(last.endMs, cue.endMs)
                        consolidated[lastIndex] = RawCue(last.startMs, newEnd, combinedText)
                    } else {
                        val adjustedPrevEnd = last.endMs.coerceAtMost(cue.startMs).coerceAtLeast(last.startMs + 200L)
                        consolidated[lastIndex] = RawCue(last.startMs, adjustedPrevEnd, last.text)
                        consolidated.add(cue)
                    }
                } else {
                    val adjustedPrevEnd = last.endMs.coerceAtMost(cue.startMs).coerceAtLeast(last.startMs + 200L)
                    consolidated[lastIndex] = RawCue(last.startMs, adjustedPrevEnd, last.text)
                    consolidated.add(cue)
                }
            }
        }

        // Split any long cues into sequential single-line cues and clamp overlapping timestamps
        val singleLineCues = mutableListOf<RawCue>()
        for ((index, cue) in consolidated.withIndex()) {
            val nextStartMs = consolidated.getOrNull(index + 1)?.startMs
            val minEndMs = cue.startMs + 1500L
            val desiredEndMs = maxOf(cue.endMs, minEndMs)
            val clampedEndMs = if (nextStartMs != null && nextStartMs > cue.startMs + 200L) {
                desiredEndMs.coerceAtMost(nextStartMs)
            } else {
                desiredEndMs
            }

            singleLineCues.addAll(splitIntoSingleLineCues(cue.startMs, clampedEndMs, cue.text))
        }

        // Format into strictly single-line WebVTT entries
        val sb = StringBuilder("WEBVTT\n\n")
        for ((index, cue) in singleLineCues.withIndex()) {
            val startTs = formatMsToTimestamp(cue.startMs)
            val endTs = formatMsToTimestamp(maxOf(cue.endMs, cue.startMs + 200L))
            val singleLineText = cue.text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()

            sb.append(index + 1).append("\n")
            sb.append(startTs).append(" --> ").append(endTs).append("\n")
            sb.append(singleLineText).append("\n\n")
        }

        return sb.toString()
    }

    private fun hasSentenceEnding(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith("!")
    }

    /**
     * Splits a cue exceeding [MAX_SINGLE_LINE_CHARS] into sequential single-line chunks
     * with proportionally distributed timestamps so every chunk fits on one line.
     */
    private fun splitIntoSingleLineCues(startMs: Long, endMs: Long, text: String): List<RawCue> {
        val clean = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= MAX_SINGLE_LINE_CHARS) {
            return listOf(RawCue(startMs, endMs, clean))
        }

        val words = clean.split(" ")
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (word in words) {
            if (currentChunk.isEmpty()) {
                currentChunk.append(word)
            } else if (currentChunk.length + 1 + word.length <= MAX_SINGLE_LINE_CHARS) {
                currentChunk.append(" ").append(word)
            } else {
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder(word)
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        if (chunks.size <= 1) {
            return listOf(RawCue(startMs, endMs, clean))
        }

        val totalDuration = (endMs - startMs).coerceAtLeast(chunks.size * 400L)
        val totalChars = chunks.sumOf { it.length }.coerceAtLeast(1)
        val result = mutableListOf<RawCue>()
        var cursorMs = startMs

        for ((i, chunk) in chunks.withIndex()) {
            val chunkEndMs = if (i == chunks.lastIndex) {
                maxOf(endMs, cursorMs + 300L)
            } else {
                val proportionalDuration = (totalDuration * chunk.length) / totalChars
                (cursorMs + proportionalDuration.coerceAtLeast(300L)).coerceAtMost(endMs - (chunks.size - 1 - i) * 200L)
            }
            val safeEndMs = maxOf(chunkEndMs, cursorMs + 200L)
            result.add(RawCue(cursorMs, safeEndMs, chunk))
            cursorMs = safeEndMs
        }

        return result
    }

    private data class RawCue(
        val startMs: Long,
        val endMs: Long,
        val text: String
    )

    private val XML_TEXT_TAG_REGEX = Pattern.compile(
        "<text[^>]*\\bstart=[\"']([0-9.]+)[\"'][^>]*?(?:\\bdur=[\"']([0-9.]+)[\"'])?[^>]*>(.*?)</text>",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )
    private val XML_P_MS_TAG_REGEX = Pattern.compile(
        "<p[^>]*\\bt=[\"'](\\d+)[\"'][^>]*?(?:\\bd=[\"'](\\d+)[\"'])?[^>]*>(.*?)</p>",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )
    private val TTML_P_TAG_REGEX = Pattern.compile(
        "<p[^>]*\\bbegin=[\"']([^\"']+)[\"'][^>]*?(?:\\b(end|dur)=[\"']([^\"']+)[\"'])?[^>]*>(.*?)</p>",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    private fun extractXmlOrTtmlCues(content: String): List<RawCue> {
        val cues = mutableListOf<RawCue>()

        // 1. TimedText <text start="1.23" dur="4.56">...</text>
        val textMatcher = XML_TEXT_TAG_REGEX.matcher(content)
        while (textMatcher.find()) {
            val startSec = textMatcher.group(1)?.toDoubleOrNull() ?: continue
            val durSec = textMatcher.group(2)?.toDoubleOrNull() ?: 2.5
            val rawBody = textMatcher.group(3) ?: ""
            val clean = cleanCueText(rawBody)
            if (clean.isNotBlank()) {
                val startMs = (startSec * 1000).toLong()
                val endMs = startMs + (durSec * 1000).toLong().coerceAtLeast(300L)
                cues.add(RawCue(startMs, endMs, clean))
            }
        }
        if (cues.isNotEmpty()) return cues

        // 2. TimedText srv3 <p t="1230" d="4560">...</p>
        val pMsMatcher = XML_P_MS_TAG_REGEX.matcher(content)
        while (pMsMatcher.find()) {
            val startMs = pMsMatcher.group(1)?.toLongOrNull() ?: continue
            val durMs = pMsMatcher.group(2)?.toLongOrNull() ?: 2500L
            val rawBody = pMsMatcher.group(3) ?: ""
            val clean = cleanCueText(rawBody)
            if (clean.isNotBlank()) {
                cues.add(RawCue(startMs, startMs + durMs.coerceAtLeast(300L), clean))
            }
        }
        if (cues.isNotEmpty()) return cues

        // 3. TTML <p begin="00:00:01.230" end="00:00:05.790">...</p>
        val ttmlMatcher = TTML_P_TAG_REGEX.matcher(content)
        while (ttmlMatcher.find()) {
            val beginStr = ttmlMatcher.group(1) ?: continue
            val attrType = ttmlMatcher.group(2)?.lowercase()
            val endOrDurStr = ttmlMatcher.group(3)
            val rawBody = ttmlMatcher.group(4) ?: ""
            val clean = cleanCueText(rawBody)
            if (clean.isNotBlank()) {
                val startMs = parseFlexibleTimeToMs(beginStr)
                val endMs = when {
                    endOrDurStr == null -> startMs + 2500L
                    attrType == "dur" -> startMs + parseFlexibleTimeToMs(endOrDurStr).coerceAtLeast(300L)
                    else -> parseFlexibleTimeToMs(endOrDurStr).coerceAtLeast(startMs + 300L)
                }
                cues.add(RawCue(startMs, endMs, clean))
            }
        }

        return cues
    }

    private fun parseFlexibleTimeToMs(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.endsWith("ms", ignoreCase = true)) {
            return trimmed.dropLast(2).toDoubleOrNull()?.toLong() ?: 0L
        }
        if (trimmed.endsWith("s", ignoreCase = true)) {
            return ((trimmed.dropLast(1).toDoubleOrNull() ?: 0.0) * 1000).toLong()
        }
        if (trimmed.contains(":")) {
            return parseTimestampToMs(trimmed)
        }
        return ((trimmed.toDoubleOrNull() ?: 0.0) * 1000).toLong()
    }

    private fun parseTimestampToMs(ts: String): Long {
        return try {
            val parts = ts.trim().split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secMs = parts[2].split(".", ",")
                val seconds = secMs[0].toLong()
                val ms = secMs.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000 + ms
            } else if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val secMs = parts[1].split(".", ",")
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
