package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.Context
import android.content.res.Resources
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import java.util.Locale

object LocaleResourceUtils {
    private val RESOURCE_PACKAGE_NAME = R::class.java.`package`!!.name

    @Volatile
    private var sInitialized = false
    private val sInitializeLock = Any()
    private lateinit var sResources: Resources
    private val sExceptionalLocaleDisplayedInRootLocale = HashMap<String, Int>()
    private val sExceptionalLocaleToNameIdsMap = HashMap<String, Int>()
    private const val LOCALE_NAME_RESOURCE_PREFIX = "string/locale_name_"
    private const val LOCALE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX = "string/locale_name_in_root_locale_"

    @Synchronized
    fun init(context: Context) {
        synchronized(sInitializeLock) {
            if (!sInitialized) {
                initLocked(context)
                sInitialized = true
            }
        }
    }

    fun onLocalChange(context: Context) {
        sResources = context.resources
    }

    private fun initLocked(context: Context) {
        val res = context.resources
        sResources = res

        val exceptionalLocaleInRootLocale = res.getStringArray(R.array.locale_displayed_in_root_locale)
        for (localeString in exceptionalLocaleInRootLocale) {
            val resourceName = LOCALE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX + localeString
            val resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME)
            sExceptionalLocaleDisplayedInRootLocale[localeString] = resId
        }

        val exceptionalLocales = res.getStringArray(R.array.locale_exception_keys)
        for (localeString in exceptionalLocales) {
            val resourceName = LOCALE_NAME_RESOURCE_PREFIX + localeString
            val resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME)
            sExceptionalLocaleToNameIdsMap[localeString] = resId
        }
    }

    private fun getDisplayLocale(localeString: String): Locale {
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            return Locale.ROOT
        }
        return LocaleUtils.constructLocaleFromString(localeString)
    }

    fun getLocaleDisplayNameInSystemLocale(localeString: String): String {
        val displayLocale = sResources.configuration.locale
        return getLocaleDisplayNameInternal(localeString, displayLocale)
    }

    fun getLocaleDisplayNameInLocale(localeString: String): String {
        val displayLocale = getDisplayLocale(localeString)
        return getLocaleDisplayNameInternal(localeString, displayLocale)
    }

    fun getLanguageDisplayNameInSystemLocale(localeString: String): String {
        val displayLocale = sResources.configuration.locale
        val languageString = if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            localeString
        } else {
            LocaleUtils.constructLocaleFromString(localeString).language
        }
        return getLocaleDisplayNameInternal(languageString, displayLocale)
    }

    fun getLanguageDisplayNameInLocale(localeString: String): String {
        val displayLocale = getDisplayLocale(localeString)
        val languageString = if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            localeString
        } else {
            LocaleUtils.constructLocaleFromString(localeString).language
        }
        return getLocaleDisplayNameInternal(languageString, displayLocale)
    }

    private fun getLocaleDisplayNameInternal(localeString: String, displayLocale: Locale): String {
        val exceptionalNameResId: Int? = when {
            displayLocale == Locale.ROOT && sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString) ->
                sExceptionalLocaleDisplayedInRootLocale[localeString]
            sExceptionalLocaleToNameIdsMap.containsKey(localeString) ->
                sExceptionalLocaleToNameIdsMap[localeString]
            else -> null
        }

        val displayName = if (exceptionalNameResId != null) {
            sResources.getString(exceptionalNameResId)
        } else {
            LocaleUtils.constructLocaleFromString(localeString).getDisplayName(displayLocale)
        }
        return StringUtils.capitalizeEachWord(displayName, displayLocale)
    }
}
