package rkr.simplekeyboard.inputmethod.latin.common

import android.content.res.Resources
import android.text.TextUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import java.util.Locale

object LocaleUtils {
    private val sLocaleCache = HashMap<String, Locale>()

    @Synchronized
    fun constructLocaleFromString(localeString: String): Locale {
        sLocaleCache[localeString]?.let { return it }
        val elements = localeString.split("_", limit = 3)
        val locale = when (elements.size) {
            1 -> Locale(elements[0])
            2 -> Locale(elements[0], elements[1])
            else -> Locale(elements[0], elements[1], elements[2])
        }
        sLocaleCache[localeString] = locale
        return locale
    }

    fun getLocaleString(locale: Locale): String {
        if (!TextUtils.isEmpty(locale.variant)) {
            return locale.language + "_" + locale.country + "_" + locale.variant
        }
        if (!TextUtils.isEmpty(locale.country)) {
            return locale.language + "_" + locale.country
        }
        return locale.language
    }

    fun findBestLocale(localeToMatch: Locale, options: Collection<Locale>): Locale? {
        for (locale in options) {
            if (locale == localeToMatch) return locale
        }
        for (locale in options) {
            if (locale.language == localeToMatch.language &&
                locale.country == localeToMatch.country &&
                locale.variant == localeToMatch.variant
            ) {
                return locale
            }
        }
        for (locale in options) {
            if (locale.language == localeToMatch.language &&
                locale.country == localeToMatch.country
            ) {
                return locale
            }
        }
        for (locale in options) {
            if (locale.language == localeToMatch.language) {
                return locale
            }
        }
        return null
    }

    fun getSystemLocales(): List<Locale> {
        val locales = ArrayList<Locale>()
        val localeList = Resources.getSystem().configuration.locales
        for (i in 0 until localeList.size()) {
            locales.add(localeList[i])
        }
        return locales
    }

    class LocaleComparator : Comparator<Locale> {
        override fun compare(a: Locale, b: Locale): Int {
            if (a == b) return 0
            val aDisplay = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(getLocaleString(a))
            val bDisplay = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(getLocaleString(b))
            val result = aDisplay.compareTo(bDisplay, ignoreCase = true)
            if (result != 0) return result
            return if (a.hashCode() > b.hashCode()) 1 else -1
        }
    }
}
