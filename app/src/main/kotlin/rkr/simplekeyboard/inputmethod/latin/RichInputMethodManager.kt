package rkr.simplekeyboard.inputmethod.latin

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import rkr.simplekeyboard.inputmethod.latin.utils.DialogUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypePreferenceUtils
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Locale
import java.util.TreeSet
import java.util.concurrent.Executors

class RichInputMethodManager private constructor() {
    private var mImmService: InputMethodManager? = null
    private lateinit var mSubtypeList: SubtypeList

    interface SubtypeChangedListener {
        fun onCurrentSubtypeChanged()
    }

    private class SubtypeList(context: Context) {
        private var mSubtypes: MutableList<Subtype> = ArrayList()
        private var mCurrentSubtypeIndex = 0
        private val mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
        private var mSubtypeChangedListener: SubtypeChangedListener? = null

        init { reload(context) }

        fun reload(context: Context) {
            val prefSubtypes = Settings.readPrefSubtypes(mPrefs)
            val subtypes = SubtypePreferenceUtils.createSubtypesFromPref(prefSubtypes, context.resources)
            mSubtypes = if (subtypes == null || subtypes.size < 1) {
                SubtypeLocaleUtils.getDefaultSubtypes(context.resources).toMutableList()
            } else {
                subtypes.toMutableList()
            }
            mCurrentSubtypeIndex = 0
        }

        fun setSubtypeChangeHandler(listener: SubtypeChangedListener?) { mSubtypeChangedListener = listener }

        fun notifySubtypeChanged() { mSubtypeChangedListener?.onCurrentSubtypeChanged() }

        @Synchronized
        fun getAllForLocale(locale: String): Set<Subtype> {
            val subtypes = HashSet<Subtype>()
            for (subtype in mSubtypes) {
                if (subtype.locale == locale) subtypes.add(subtype)
            }
            return subtypes
        }

        @Synchronized
        fun getAll(sortForDisplay: Boolean): Set<Subtype> {
            val subtypes: MutableSet<Subtype> = if (sortForDisplay) {
                TreeSet { a, b ->
                    if (a == b) 0
                    else {
                        val result = a.name.compareTo(b.name, ignoreCase = true)
                        if (result != 0) result else if (a.hashCode() > b.hashCode()) 1 else -1
                    }
                }
            } else {
                HashSet()
            }
            subtypes.addAll(mSubtypes)
            return subtypes
        }

        @Synchronized
        fun size(): Int = mSubtypes.size

        private fun saveSubtypeListPref() {
            Settings.writePrefSubtypes(mPrefs, SubtypePreferenceUtils.createPrefSubtypes(mSubtypes))
        }

        @Synchronized
        fun addSubtype(subtype: Subtype): Boolean {
            if (mSubtypes.contains(subtype)) return true
            if (!mSubtypes.add(subtype)) return false
            saveSubtypeListPref()
            return true
        }

        @Synchronized
        fun removeSubtype(subtype: Subtype): Boolean {
            if (mSubtypes.size == 1) return false
            val index = mSubtypes.indexOf(subtype)
            if (index < 0) return true
            val subtypeChanged = if (mCurrentSubtypeIndex == index) {
                mCurrentSubtypeIndex = 0
                true
            } else {
                if (mCurrentSubtypeIndex > index) mCurrentSubtypeIndex--
                false
            }
            mSubtypes.removeAt(index)
            saveSubtypeListPref()
            if (subtypeChanged) notifySubtypeChanged()
            return true
        }

        @Synchronized
        fun resetSubtypeCycleOrder() {
            if (mCurrentSubtypeIndex == 0) return
            Collections.rotate(mSubtypes.subList(0, mCurrentSubtypeIndex + 1), 1)
            mCurrentSubtypeIndex = 0
            saveSubtypeListPref()
        }

        @Synchronized
        fun setCurrentSubtype(subtype: Subtype): Boolean {
            if (getCurrentSubtype() == subtype) return true
            for (i in mSubtypes.indices) {
                if (mSubtypes[i] == subtype) { setCurrentSubtype(i); return true }
            }
            return false
        }

        @Synchronized
        fun setCurrentSubtype(locale: Locale): Boolean {
            val enabledLocales = ArrayList<Locale>(mSubtypes.size)
            for (subtype in mSubtypes) enabledLocales.add(subtype.localeObject!!)
            val bestLocale = LocaleUtils.findBestLocale(locale, enabledLocales)
            if (bestLocale != null) {
                for (i in mSubtypes.indices) {
                    if (bestLocale == mSubtypes[i].localeObject) { setCurrentSubtype(i); return true }
                }
            }
            return false
        }

        private fun setCurrentSubtype(index: Int) {
            if (mCurrentSubtypeIndex == index) return
            mCurrentSubtypeIndex = index
            if (index != 0) resetSubtypeCycleOrder()
            notifySubtypeChanged()
        }

        @Synchronized
        fun switchToNextSubtype(notifyChangeOnCycle: Boolean): Boolean {
            val nextIndex = mCurrentSubtypeIndex + 1
            if (nextIndex >= mSubtypes.size) {
                mCurrentSubtypeIndex = 0
                if (!notifyChangeOnCycle) return false
            } else {
                mCurrentSubtypeIndex = nextIndex
            }
            notifySubtypeChanged()
            return true
        }

        @Synchronized
        fun getCurrentSubtype(): Subtype = mSubtypes[mCurrentSubtypeIndex]
    }

    fun init(context: Context) {
        if (mImmService != null) return
        mImmService = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        LocaleResourceUtils.init(context)
        mSubtypeList = SubtypeList(context)
    }

    fun reloadSubtypes(context: Context) { mSubtypeList.reload(context) }
    fun setSubtypeChangeHandler(listener: SubtypeChangedListener?) { mSubtypeList.setSubtypeChangeHandler(listener) }
    fun getEnabledSubtypes(sortForDisplay: Boolean): Set<Subtype> = mSubtypeList.getAll(sortForDisplay)
    fun getEnabledSubtypesForLocale(locale: String): Set<Subtype> = mSubtypeList.getAllForLocale(locale)
    fun hasMultipleEnabledSubtypes(): Boolean = mSubtypeList.size() > 1
    fun addSubtype(subtype: Subtype): Boolean = mSubtypeList.addSubtype(subtype)
    fun removeSubtype(subtype: Subtype): Boolean = mSubtypeList.removeSubtype(subtype)
    fun resetSubtypeCycleOrder() { mSubtypeList.resetSubtypeCycleOrder() }
    fun setCurrentSubtype(subtype: Subtype): Boolean = mSubtypeList.setCurrentSubtype(subtype)
    fun setCurrentSubtype(locale: Locale): Boolean = mSubtypeList.setCurrentSubtype(locale)
    fun getCurrentSubtype(): Subtype = mSubtypeList.getCurrentSubtype()

    fun switchToNextInputMethod(token: IBinder?, onlyCurrentIme: Boolean): Boolean {
        if (onlyCurrentIme) {
            if (!hasMultipleEnabledSubtypes()) return false
            return mSubtypeList.switchToNextSubtype(true)
        }
        if (mSubtypeList.switchToNextSubtype(false)) return true
        if (mImmService!!.switchToNextInputMethod(token, false)) return true
        if (hasMultipleEnabledSubtypes()) { mSubtypeList.notifySubtypeChanged(); return true }
        return false
    }

    fun shouldOfferSwitchingToOtherInputMethods(binder: IBinder?): Boolean = mImmService!!.shouldOfferSwitchingToNextInputMethod(binder)

    private class SubtypeInfo {
        var systemSubtype: InputMethodSubtype? = null
        var virtualSubtype: Subtype? = null
        var subtypeName: CharSequence? = null
        var imeName: CharSequence? = null
        var imiId: String? = null
    }

    companion object {
        private const val TAG = "RichInputMethodManager"
        private val sInstance = RichInputMethodManager()
        fun getInstance(): RichInputMethodManager { sInstance.checkInitialized(); return sInstance }
        fun init(context: Context) { sInstance.init(context) }
    }

    private fun isInitialized(): Boolean = mImmService != null
    private fun checkInitialized() { if (!isInitialized()) throw RuntimeException("$TAG is used before initialization") }
}
