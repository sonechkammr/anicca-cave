package com.lifeclock.widget.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * The city's name in [locale] ("Таллин"), from Android's ICU data. Falls back to the English name taken from the zone
 * id when ICU has no entry. ICU is the platform's own copy, so no data ships in the app.
 */
fun localizedCityName(zoneId: String, locale: java.util.Locale): String {
    val name = runCatching { android.icu.text.TimeZoneNames.getInstance(locale).getExemplarLocationName(zoneId) }.getOrNull()
    return if (name.isNullOrBlank()) Cities.nameOf(zoneId) else name
}

/** False for legacy aliases such as `Australia/ACT` or `America/Atka`, which ICU maps to another zone. */
fun isCanonicalZone(zoneId: String): Boolean =
    runCatching { android.icu.util.TimeZone.getCanonicalID(zoneId) == zoneId }.getOrDefault(true)

/** Per-app language, through the platform's LocaleManager (Android 13 and newer). */
object AppLanguage {
    /** Android 12 has no per-app language API, so the switch is hidden there. */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** "" follows the system, else a language tag such as "en" or "ru". */
    fun current(ctx: Context): String {
        if (!supported) return ""
        val locales = ctx.getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) "" else locales[0].language
    }

    /** Changing it recreates the activity; state lives in the ViewModel and survives. */
    fun set(ctx: Context, tag: String) {
        if (!supported) return
        ctx.getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}
