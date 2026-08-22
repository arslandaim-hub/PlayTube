/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import android.content.Context
import android.content.res.Configuration
import com.arslandaim.playtube.R
import java.util.Locale

object LocaleUtils {

    /**
     * Dynamically discovers all locales that have app-specific translations.
     * It filters out library-provided locales by checking a sentinel string.
     */
    fun getAvailableLocales(context: Context): List<Locale> {
        val assetLocales = context.assets.locales
        val supportedLocales = mutableListOf<Locale>()
        
        // A string that is unique to PlayTube and unlikely to be in a library
        val sentinelString = context.getString(R.string.background_play)

        for (tag in assetLocales) {
            if (tag.isEmpty()) continue
            val locale = Locale.forLanguageTag(tag)
            
            if (hasAppTranslation(context, locale, sentinelString)) {
                supportedLocales.add(locale)
            }
        }

        // Ensure English is always available
        if (supportedLocales.none { it.language == "en" }) {
            supportedLocales.add(Locale.ENGLISH)
        }

        return supportedLocales
            .distinctBy { it.language }
            .sortedBy { it.getDisplayLanguage(it).replaceFirstChar { c -> c.uppercase() } }
    }

    private fun hasAppTranslation(context: Context, locale: Locale, defaultString: String): Boolean {
        return try {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localeContext = context.createConfigurationContext(config)
            val translatedString = localeContext.getString(R.string.background_play)
            
            // If it's English, we assume it's supported
            if (locale.language == "en") return true
            
            // If the translated string is different from the default (which is usually English in this app's context)
            // or if it's the same but the locale is NOT English, it's a bit ambiguous.
            // However, AssetManager.getLocales() only returns locales that have SOME resources.
            // Most library locales won't have 'background_play' translated.
            translatedString != defaultString || locale.language == Locale.getDefault().language
        } catch (e: Exception) {
            false
        }
    }

    fun getDisplayName(locale: Locale): String {
        return locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
    }
}
