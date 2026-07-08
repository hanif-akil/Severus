package rkr.simplekeyboard.inputmethod.latin.utils

import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.Subtype

object LanguageOnSpacebarUtils {
    const val FORMAT_TYPE_NONE = 0
    const val FORMAT_TYPE_LANGUAGE_ONLY = 1
    const val FORMAT_TYPE_FULL_LOCALE = 2

    fun getLanguageOnSpacebarFormatType(subtype: Subtype): Int {
        val locale = subtype.localeObject ?: return FORMAT_TYPE_NONE
        val keyboardLanguage = locale.language
        val keyboardLayout = subtype.keyboardLayoutSet
        var sameLanguageAndLayoutCount = 0
        val enabledSubtypes = RichInputMethodManager.getInstance().getEnabledSubtypes(false)
        for (enabledSubtype in enabledSubtypes) {
            val language = enabledSubtype.localeObject!!.language
            if (keyboardLanguage == language && keyboardLayout == enabledSubtype.keyboardLayoutSet) {
                sameLanguageAndLayoutCount++
            }
        }
        return if (sameLanguageAndLayoutCount > 1) FORMAT_TYPE_FULL_LOCALE else FORMAT_TYPE_LANGUAGE_ONLY
    }
}
