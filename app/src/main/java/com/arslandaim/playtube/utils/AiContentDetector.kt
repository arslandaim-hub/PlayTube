/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import com.arslandaim.playtube.domain.model.VideoItem

/**
 * High-precision detector for identifying AI-generated videos and AI channels.
 */
object AiContentDetector {

    private val AI_TITLE_REGEX = Regex(
        "(?i)\\b(ai\\s+generated|a\\.i\\.\\s+generated|ai-generated|a\\.i\\.-generated|generated\\s+by\\s+ai|generated\\s+with\\s+ai|created\\s+with\\s+ai|created\\s+by\\s+ai|made\\s+by\\s+ai|made\\s+with\\s+ai|ai\\s+created|ai-created|ai\\s+cover|a\\.i\\.\\s+cover|ai\\s+song|a\\.i\\.\\s+song|ai\\s+music|a\\.i\\.\\s+music|ai\\s+video|a\\.i\\.\\s+video|ai\\s+art|ai\\s+animation|ai\\s+avatar|ai\\s+voice|a\\.i\\.\\s+voice|ai\\s+voiceover|a\\.i\\.\\s+voiceover|ai\\s+shorts|ai\\s+film|ai\\s+movie|ai\\s+news|ai\\s+story|ai\\s+stories|sora\\s+ai|midjourney|suno\\s+ai|suno|udio\\s+ai|udio|runwayml|runway\\s+gen|pika\\s+ai|elevenlabs|dall-e|dalle|stable\\s+diffusion|chatgpt|gpt-4|claude\\s+ai|luma\\s+dream|kling\\s+ai|veo\\s+2|flux\\.1)\\b|#aigenerated|#aigeneratedcontent|#aicover|#aimusic|#aivideo|#aishorts|#aiart|#sunoai|#udioai"
    )

    private val AI_CHANNEL_REGEX = Regex(
        "(?i)\\b(ai|a\\.i\\.)\\b.*\\b(generated|cover|music|song|songs|video|videos|animation|animations|art|voice|voices|voiceover|story|stories|shorts|studio|studioworks|creations|lab|labs|channel|world|zone|realm|media|content|news|films|hub|daily|digest|central|tv|official|plus|club|universe)\\b|\\b(generated|cover|music|song|songs|video|videos|animation|animations|art|voice|voices|voiceover|story|stories|shorts|studio|studioworks|creations|lab|labs|channel|world|zone|realm|media|content|news|films|hub|daily|digest|central|tv|official|plus|club|universe)\\b.*\\b(ai|a\\.i\\.)\\b|(?i)\\b(ai\\s+generated|a\\.i\\.\\s+generated|midjourney|sora|suno|udio|runwayml|pika\\s+ai|elevenlabs|dall-e|stable\\s+diffusion)\\b"
    )

    fun isAiContent(video: VideoItem): Boolean {
        if (isAiTitle(video.title)) return true
        if (isAiChannel(video.uploaderName)) return true
        return false
    }

    fun isAiTitle(title: String): Boolean {
        return AI_TITLE_REGEX.containsMatchIn(title)
    }

    fun isAiChannel(uploaderName: String): Boolean {
        return AI_CHANNEL_REGEX.containsMatchIn(uploaderName)
    }
}
