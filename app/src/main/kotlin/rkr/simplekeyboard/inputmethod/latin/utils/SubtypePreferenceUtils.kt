package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.res.Resources
import android.text.TextUtils
import android.util.Log
import rkr.simplekeyboard.inputmethod.latin.Subtype

object SubtypePreferenceUtils {
    private const val TAG = "SubtypePreferenceUtils"
    private const val LOCALE_AND_LAYOUT_SEPARATOR = ":"
    private const val INDEX_OF_LOCALE = 0
    private const val INDEX_OF_KEYBOARD_LAYOUT = 1
    private const val PREF_ELEMENTS_LENGTH = INDEX_OF_KEYBOARD_LAYOUT + 1
    private const val PREF_SUBTYPE_SEPARATOR = ";"

    private fun getPrefString(subtype: Subtype): String {
        val localeString = subtype.locale
        val keyboardLayoutSetName = subtype.keyboardLayoutSet
        return "$localeString$LOCALE_AND_LAYOUT_SEPARATOR$keyboardLayoutSetName"
    }

    fun createSubtypesFromPref(prefSubtypes: String, resources: Resources): List<Subtype> {
        Log.i(TAG, "Loading subtypes: $prefSubtypes")
        if (TextUtils.isEmpty(prefSubtypes)) return ArrayList()
        val prefSubtypeArray = prefSubtypes.split(PREF_SUBTYPE_SEPARATOR)
        val subtypesList = ArrayList<Subtype>(prefSubtypeArray.size)
        for (prefSubtype in prefSubtypeArray) {
            val elements = prefSubtype.split(LOCALE_AND_LAYOUT_SEPARATOR)
            if (elements.size != PREF_ELEMENTS_LENGTH) {
                Log.w(TAG, "Unknown subtype specified: $prefSubtype in $prefSubtypes")
                continue
            }
            val localeString = elements[INDEX_OF_LOCALE]
            val keyboardLayoutSetName = elements[INDEX_OF_KEYBOARD_LAYOUT]
            val subtype = SubtypeLocaleUtils.getSubtype(localeString, keyboardLayoutSetName, resources)
            if (subtype != null) subtypesList.add(subtype)
        }
        return subtypesList
    }

    fun createPrefSubtypes(subtypes: List<Subtype>?): String {
        if (subtypes == null || subtypes.isEmpty()) return ""
        val sb = StringBuilder()
        for (subtype in subtypes) {
            if (sb.length > 0) sb.append(PREF_SUBTYPE_SEPARATOR)
            sb.append(getPrefString(subtype))
        }
        return sb.toString()
    }
}
