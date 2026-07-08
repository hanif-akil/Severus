package rkr.simplekeyboard.inputmethod.latin

import android.content.res.Resources
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import java.util.Locale

class Subtype {
    private val mLocale: String
    private val mLayoutSet: String
    private val mLayoutNameRes: Int
    private val mLayoutNameStr: String?
    private val mShowLayoutInName: Boolean
    private val mResources: Resources

    constructor(locale: String, layoutSet: String, layoutNameRes: Int, showLayoutInName: Boolean, resources: Resources) {
        mLocale = locale
        mLayoutSet = layoutSet
        mLayoutNameRes = layoutNameRes
        mLayoutNameStr = null
        mShowLayoutInName = showLayoutInName
        mResources = resources
    }

    constructor(locale: String, layoutSet: String, layoutNameStr: String?, showLayoutInName: Boolean, resources: Resources) {
        mLocale = locale
        mLayoutSet = layoutSet
        mLayoutNameRes = NO_RESOURCE
        mLayoutNameStr = layoutNameStr
        mShowLayoutInName = showLayoutInName
        mResources = resources
    }

    val locale: String get() = mLocale
    val localeObject: Locale? get() = LocaleUtils.constructLocaleFromString(mLocale)
    val keyboardLayoutSet: String get() = mLayoutSet

    val name: String
        get() {
            val localeDisplayName = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(mLocale)
            if (mShowLayoutInName) {
                if (mLayoutNameRes != NO_RESOURCE) {
                    return mResources.getString(R.string.subtype_generic_layout, localeDisplayName, mResources.getString(mLayoutNameRes))
                }
                if (mLayoutNameStr != null) {
                    return mResources.getString(R.string.subtype_generic_layout, localeDisplayName, mLayoutNameStr)
                }
            }
            return localeDisplayName
        }

    val layoutDisplayName: String
        get() = when {
            mLayoutNameRes != NO_RESOURCE -> mResources.getString(mLayoutNameRes)
            mLayoutNameStr != null -> mLayoutNameStr
            else -> LocaleResourceUtils.getLanguageDisplayNameInSystemLocale(mLocale)
        }

    override fun equals(other: Any?): Boolean {
        if (other !is Subtype) return false
        return mLocale == other.mLocale && mLayoutSet == other.mLayoutSet
    }

    override fun hashCode(): Int {
        var hashCode = 31 + mLocale.hashCode()
        hashCode = hashCode * 31 + mLayoutSet.hashCode()
        return hashCode
    }

    override fun toString(): String = "subtype $mLocale:$mLayoutSet"

    companion object {
        private const val NO_RESOURCE = 0
    }
}
