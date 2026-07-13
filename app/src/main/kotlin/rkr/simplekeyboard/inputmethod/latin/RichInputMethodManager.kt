/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
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
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypePreferenceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.Collections
import java.util.Locale
import java.util.TreeSet
import java.util.concurrent.Executors

class RichInputMethodManager private constructor() {

    interface SubtypeChangedListener {
        fun onCurrentSubtypeChanged()
    }

    private class SubtypeList(context: Context) {
        private var mSubtypes: MutableList<Subtype> = mutableListOf()
        private var mCurrentSubtypeIndex = 0
        private val mPrefs: SharedPreferences = PreferenceManagerCompat.getDeviceSharedPreferences(context)
        private var mSubtypeChangedListener: SubtypeChangedListener? = null

        init {
            reload(context)
        }

        fun reload(context: Context) {
            val prefSubtypes = Settings.readPrefSubtypes(mPrefs)
            val subtypes = SubtypePreferenceUtils.createSubtypesFromPref(prefSubtypes, context.resources)
            mSubtypes = if (subtypes == null || subtypes.size < 1) {
                SubtypeLocaleUtils.getDefaultSubtypes(context.resources)
            } else {
                subtypes.toMutableList()
            }
            mCurrentSubtypeIndex = 0
        }

        fun setSubtypeChangeHandler(listener: SubtypeChangedListener?) {
            mSubtypeChangedListener = listener
        }

        fun notifySubtypeChanged() {
            mSubtypeChangedListener?.onCurrentSubtypeChanged()
        }

        @Synchronized
        fun getAllForLocale(locale: String): Set<Subtype> {
            val subtypes = mutableSetOf<Subtype>()
            for (subtype in mSubtypes) {
                if (subtype.getLocale() == locale) {
                    subtypes.add(subtype)
                }
            }
            return subtypes
        }

        @Synchronized
        fun getAll(sortForDisplay: Boolean): Set<Subtype> {
            val subtypes: MutableSet<Subtype> = if (sortForDisplay) {
                object : TreeSet<Subtype>() {
                    init {
                        object : Comparator<Subtype> {
                            override fun compare(a: Subtype, b: Subtype): Int {
                                if (a == b) {
                                    return 0
                                }
                                val result = a.getName().compareToIgnoreCase(b.getName())
                                if (result != 0) {
                                    return result
                                }
                                return if (a.hashCode() > b.hashCode()) 1 else -1
                            }
                        }.let { comparator -> TreeSet(it) }
                    }
                }.also { it.addAll(mSubtypes) }
            } else {
                mutableSetOf()
            }
            subtypes.addAll(mSubtypes)
            return subtypes
        }

        @Synchronized
        fun size(): Int {
            return mSubtypes.size
        }

        private fun saveSubtypeListPref() {
            val prefSubtypes = SubtypePreferenceUtils.createPrefSubtypes(mSubtypes)
            Settings.writePrefSubtypes(mPrefs, prefSubtypes)
        }

        @Synchronized
        fun addSubtype(subtype: Subtype): Boolean {
            if (mSubtypes.contains(subtype)) {
                return true
            }
            if (!mSubtypes.add(subtype)) {
                return false
            }
            saveSubtypeListPref()
            return true
        }

        @Synchronized
        fun removeSubtype(subtype: Subtype): Boolean {
            if (mSubtypes.size == 1) {
                return false
            }

            val index = mSubtypes.indexOf(subtype)
            if (index < 0) {
                return true
            }

            val subtypeChanged: Boolean
            if (mCurrentSubtypeIndex == index) {
                mCurrentSubtypeIndex = 0
                subtypeChanged = true
            } else {
                if (mCurrentSubtypeIndex > index) {
                    mCurrentSubtypeIndex--
                }
                subtypeChanged = false
            }

            mSubtypes.removeAt(index)
            saveSubtypeListPref()
            if (subtypeChanged) {
                notifySubtypeChanged()
            }
            return true
        }

        @Synchronized
        fun resetSubtypeCycleOrder() {
            if (mCurrentSubtypeIndex == 0) {
                return
            }

            Collections.rotate(mSubtypes.subList(0, mCurrentSubtypeIndex + 1), 1)
            mCurrentSubtypeIndex = 0
            saveSubtypeListPref()
        }

        @Synchronized
        fun setCurrentSubtype(subtype: Subtype): Boolean {
            if (getCurrentSubtype() == subtype) {
                return true
            }
            for (i in mSubtypes.indices) {
                if (mSubtypes[i] == subtype) {
                    setCurrentSubtype(i)
                    return true
                }
            }
            return false
        }

        @Synchronized
        fun setCurrentSubtype(locale: Locale): Boolean {
            val enabledLocales = ArrayList<Locale>(mSubtypes.size)
            for (subtype in mSubtypes) {
                enabledLocales.add(subtype.getLocaleObject())
            }
            val bestLocale = LocaleUtils.findBestLocale(locale, enabledLocales)
            if (bestLocale != null) {
                for (i in mSubtypes.indices) {
                    val subtype = mSubtypes[i]
                    if (bestLocale == subtype.getLocaleObject()) {
                        setCurrentSubtype(i)
                        return true
                    }
                }
            }
            return false
        }

        private fun setCurrentSubtype(index: Int) {
            if (mCurrentSubtypeIndex == index) {
                return
            }
            mCurrentSubtypeIndex = index
            if (index != 0) {
                resetSubtypeCycleOrder()
            }
            notifySubtypeChanged()
        }

        @Synchronized
        fun switchToNextSubtype(notifyChangeOnCycle: Boolean): Boolean {
            val nextIndex = mCurrentSubtypeIndex + 1
            if (nextIndex >= mSubtypes.size) {
                mCurrentSubtypeIndex = 0
                if (!notifyChangeOnCycle) {
                    return false
                }
            } else {
                mCurrentSubtypeIndex = nextIndex
            }
            notifySubtypeChanged()
            return true
        }

        @Synchronized
        fun getCurrentSubtype(): Subtype {
            return mSubtypes[mCurrentSubtypeIndex]
        }
    }

    private class SubtypeInfo {
        var systemSubtype: InputMethodSubtype? = null
        var virtualSubtype: Subtype? = null
        var subtypeName: CharSequence? = null
        var imeName: CharSequence? = null
        var imiId: String? = null
    }

    private var mImmService: InputMethodManager? = null
    private lateinit var mSubtypeList: SubtypeList

    private fun isInitialized(): Boolean {
        return ::mSubtypeList.isInitialized && mImmService != null
    }

    private fun checkInitialized() {
        if (!isInitialized()) {
            throw RuntimeException("$TAG is used before initialization")
        }
    }

    private fun initInternal(context: Context) {
        if (isInitialized()) {
            return
        }
        @Suppress("UNCHECKED_CAST")
        mImmService = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        LocaleResourceUtils.init(context)

        mSubtypeList = SubtypeList(context)
    }

    fun reloadSubtypes(context: Context) {
        mSubtypeList.reload(context)
    }

    fun setSubtypeChangeHandler(listener: SubtypeChangedListener?) {
        mSubtypeList.setSubtypeChangeHandler(listener)
    }

    fun getEnabledSubtypes(sortForDisplay: Boolean): Set<Subtype> {
        return mSubtypeList.getAll(sortForDisplay)
    }

    fun getEnabledSubtypesForLocale(locale: String): Set<Subtype> {
        return mSubtypeList.getAllForLocale(locale)
    }

    fun hasMultipleEnabledSubtypes(): Boolean {
        return mSubtypeList.size() > 1
    }

    fun addSubtype(subtype: Subtype): Boolean {
        return mSubtypeList.addSubtype(subtype)
    }

    fun removeSubtype(subtype: Subtype): Boolean {
        return mSubtypeList.removeSubtype(subtype)
    }

    fun resetSubtypeCycleOrder() {
        mSubtypeList.resetSubtypeCycleOrder()
    }

    fun setCurrentSubtype(subtype: Subtype): Boolean {
        return mSubtypeList.setCurrentSubtype(subtype)
    }

    fun setCurrentSubtype(locale: Locale): Boolean {
        return mSubtypeList.setCurrentSubtype(locale)
    }

    fun switchToNextInputMethod(token: IBinder?, onlyCurrentIme: Boolean): Boolean {
        if (onlyCurrentIme) {
            if (!hasMultipleEnabledSubtypes()) {
                return false
            }
            return mSubtypeList.switchToNextSubtype(true)
        }
        if (mSubtypeList.switchToNextSubtype(false)) {
            return true
        }
        if (mImmService?.switchToNextInputMethod(token, false) == true) {
            return true
        }
        if (hasMultipleEnabledSubtypes()) {
            mSubtypeList.notifySubtypeChanged()
            return true
        }
        return false
    }

    fun getCurrentSubtype(): Subtype {
        return mSubtypeList.getCurrentSubtype()
    }

    fun shouldOfferSwitchingToOtherInputMethods(binder: IBinder?): Boolean {
        return mImmService?.shouldOfferSwitchingToNextInputMethod(binder) ?: false
    }

    fun showSubtypePicker(
        context: Context,
        windowToken: IBinder?,
        inputMethodService: InputMethodService
    ): AlertDialog? {
        if (windowToken == null) {
            return null
        }
        val title = context.getString(R.string.change_keyboard)

        val subtypeInfoList = getEnabledSubtypeInfoOfAllImes(context)
        if (subtypeInfoList.size < 2) {
            return null
        }

        val items = arrayOfNulls<CharSequence>(subtypeInfoList.size)
        val currentSubtype = getCurrentSubtype()
        var currentSubtypeIndex = 0
        var i = 0
        for (subtypeInfo in subtypeInfoList) {
            if (subtypeInfo.virtualSubtype != null && subtypeInfo.virtualSubtype == currentSubtype) {
                currentSubtypeIndex = i
            }

            val itemTitle: SpannableString
            val itemSubtitle: SpannableString
            if (!TextUtils.isEmpty(subtypeInfo.subtypeName)) {
                itemTitle = SpannableString(subtypeInfo.subtypeName)
                itemSubtitle = SpannableString("\n" + subtypeInfo.imeName)
            } else {
                itemTitle = SpannableString(subtypeInfo.imeName)
                itemSubtitle = SpannableString("")
            }
            itemTitle.setSpan(
                RelativeSizeSpan(0.9f), 0, itemTitle.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            itemSubtitle.setSpan(
                RelativeSizeSpan(0.85f), 0, itemSubtitle.length,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )

            items[i++] = SpannableStringBuilder().append(itemTitle).append(itemSubtitle)
        }
        val listener = DialogInterface.OnClickListener { di, position ->
            di.dismiss()
            var j = 0
            for (subtypeInfo in subtypeInfoList) {
                if (j == position) {
                    if (subtypeInfo.virtualSubtype != null) {
                        setCurrentSubtype(subtypeInfo.virtualSubtype!!)
                    } else {
                        switchToTargetIme(subtypeInfo.imiId, subtypeInfo.systemSubtype, inputMethodService)
                    }
                    break
                }
                j++
            }
        }
        val builder = AlertDialog.Builder(DialogUtils.getPlatformDialogThemeContext(context))
        builder.setSingleChoiceItems(items, currentSubtypeIndex, listener).setTitle(title)
        val dialog = builder.create()
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val window = dialog.window
        val lp = window?.attributes
        if (lp != null) {
            lp.token = windowToken
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            window.attributes = lp
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }

        dialog.show()
        return dialog
    }

    private fun getEnabledSubtypeInfoOfAllImes(context: Context): List<SubtypeInfo> {
        val subtypeInfoList = mutableListOf<SubtypeInfo>()
        val packageManager = context.packageManager

        val imiList = TreeSet<InputMethodInfo> { a, b ->
            if (a == b) {
                return@TreeSet 0
            }
            val labelA = a.loadLabel(packageManager).toString()
            val labelB = b.loadLabel(packageManager).toString()
            val result = labelA.compareToIgnoreCase(labelB)
            if (result != 0) {
                result
            } else {
                if (a.hashCode() > b.hashCode()) 1 else -1
            }
        }
        imiList.addAll(mImmService?.enabledInputMethodList ?: emptyList())

        for (imi in imiList) {
            val imeName = imi.loadLabel(packageManager)
            val imiId = imi.id
            val packageName = imi.packageName

            if (packageName == context.packageName) {
                for (subtype in getEnabledSubtypes(true)) {
                    val subtypeInfo = SubtypeInfo().apply {
                        virtualSubtype = subtype
                        subtypeName = subtype.getName()
                        this.imeName = imeName
                        this.imiId = imiId
                    }
                    subtypeInfoList.add(subtypeInfo)
                }
                continue
            }

            val subtypes = mImmService?.getEnabledInputMethodSubtypeList(imi, true) ?: emptyList()
            if (subtypes.isEmpty()) {
                val subtypeInfo = SubtypeInfo().apply {
                    this.imeName = imeName
                    this.imiId = imiId
                }
                subtypeInfoList.add(subtypeInfo)
                continue
            }

            val applicationInfo = imi.serviceInfo?.applicationInfo
            for (subtype in subtypes) {
                if (subtype.isAuxiliary) {
                    continue
                }
                val subtypeInfo = SubtypeInfo().apply {
                    systemSubtype = subtype
                    if (!subtype.overridesImplicitlyEnabledSubtype()) {
                        subtypeName = subtype.getDisplayName(context, packageName, applicationInfo)
                    }
                    this.imeName = imeName
                    this.imiId = imiId
                }
                subtypeInfoList.add(subtypeInfo)
            }
        }

        return subtypeInfoList
    }

    private fun switchToTargetIme(
        imiId: String?,
        subtype: InputMethodSubtype?,
        context: InputMethodService
    ) {
        val token = context.window.window?.attributes?.token ?: return
        val imm = mImmService
        Executors.newSingleThreadExecutor().execute {
            imm?.setInputMethodAndSubtype(token, imiId, subtype)
        }
    }

    companion object {
        private val TAG = RichInputMethodManager::class.java.simpleName

        private val sInstance = RichInputMethodManager()

        @JvmStatic
        fun getInstance(): RichInputMethodManager {
            sInstance.checkInitialized()
            return sInstance
        }

        @JvmStatic
        fun init(context: Context) {
            sInstance.initInternal(context)
        }
    }
}
