/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2021 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.latin.settings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.MenuItemIconColorCompat
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.Locale
import java.util.SortedSet
import java.util.TreeSet

class LanguagesSettingsFragment : PreferenceFragment() {
    private var mRichImm: RichInputMethodManager? = null
    private var mUsedLocaleNames: Array<CharSequence>? = null
    private var mUsedLocaleValues: Array<String>? = null
    private var mUnusedLocaleNames: Array<CharSequence>? = null
    private var mUnusedLocaleValues: Array<String>? = null
    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        RichInputMethodManager.init(activity)
        mRichImm = RichInputMethodManager.getInstance()

        addPreferencesFromResource(R.xml.empty_settings)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mView = super.onCreateView(inflater, container, savedInstanceState)
        return mView!!
    }

    override fun onStart() {
        super.onStart()
        buildContent()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.remove_language, menu)
        inflater.inflate(R.menu.add_language, menu)

        val addLanguageMenuItem = menu.findItem(R.id.action_add_language)
        MenuItemIconColorCompat.matchMenuIconColor(mView!!, addLanguageMenuItem, activity.actionBar)
        val removeLanguageMenuItem = menu.findItem(R.id.action_remove_language)
        MenuItemIconColorCompat.matchMenuIconColor(mView!!, removeLanguageMenuItem, activity.actionBar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_add_language) {
            showAddLanguagePopup()
        } else if (itemId == R.id.action_remove_language) {
            showRemoveLanguagePopup()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        mUsedLocaleNames?.let {
            menu.findItem(R.id.action_remove_language).isVisible = it.size > 1
        }
    }

    private fun buildContent() {
        val context = activity
        val group = preferenceScreen
        group.removeAll()

        val languageCategory = PreferenceCategory(context)
        languageCategory.setTitle(R.string.user_languages)
        group.addPreference(languageCategory)

        val comparator = LocaleUtils.LocaleComparator()
        val enabledSubtypes = mRichImm!!.getEnabledSubtypes(false)
        val usedLocales = getUsedLocales(enabledSubtypes, comparator)
        val unusedLocales = getUnusedLocales(usedLocales, comparator)

        buildLanguagePreferences(usedLocales, group, context)
        setLocaleEntries(usedLocales, unusedLocales)
    }

    private fun getUsedLocales(
        subtypes: Set<Subtype>,
        comparator: java.util.Comparator<Locale>
    ): SortedSet<Locale> {
        val locales = TreeSet(comparator)
        for (subtype in subtypes) {
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format(
                    "Enabled subtype: %-6s 0x%08x %11d %s",
                    subtype.locale, subtype.hashCode(), subtype.hashCode(), subtype.name
                ))
            }
            locales.add(subtype.localeObject)
        }
        return locales
    }

    private fun getUnusedLocales(
        usedLocales: Set<Locale>,
        comparator: java.util.Comparator<Locale>
    ): SortedSet<Locale> {
        val locales = TreeSet(comparator)
        for (localeString in SubtypeLocaleUtils.getSupportedLocales()) {
            val locale = LocaleUtils.constructLocaleFromString(localeString)
            if (usedLocales.contains(locale)) {
                continue
            }
            locales.add(locale)
        }
        return locales
    }

    private fun buildLanguagePreferences(
        locales: SortedSet<Locale>,
        group: android.preference.PreferenceGroup,
        context: Context
    ) {
        for (locale in locales) {
            val localeString = LocaleUtils.getLocaleString(locale)
            val pref = SingleLanguagePreference(context, localeString)
            group.addPreference(pref)
        }
    }

    private fun setLocaleEntries(usedLocales: SortedSet<Locale>, unusedLocales: SortedSet<Locale>) {
        mUsedLocaleNames = Array(usedLocales.size) { "" }
        mUsedLocaleValues = Array(usedLocales.size) { "" }
        var i = 0
        for (locale in usedLocales) {
            val localeString = LocaleUtils.getLocaleString(locale)
            mUsedLocaleValues!![i] = localeString
            mUsedLocaleNames!![i] = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString)
            i++
        }

        mUnusedLocaleNames = Array(unusedLocales.size) { "" }
        mUnusedLocaleValues = Array(unusedLocales.size) { "" }
        i = 0
        for (locale in unusedLocales) {
            val localeString = LocaleUtils.getLocaleString(locale)
            mUnusedLocaleValues!![i] = localeString
            mUnusedLocaleNames!![i] = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString)
            i++
        }
    }

    private fun showAddLanguagePopup() {
        showMultiChoiceDialog(mUnusedLocaleNames!!, R.string.add_language, R.string.add, true) { checkedItems ->
            for (i in checkedItems.indices) {
                if (!checkedItems[i]) continue
                val subtype = SubtypeLocaleUtils.getDefaultSubtype(
                    mUnusedLocaleValues!![i],
                    this@LanguagesSettingsFragment.resources
                )
                mRichImm!!.addSubtype(subtype)
            }
            activity.invalidateOptionsMenu()
            buildContent()
        }
    }

    private fun showRemoveLanguagePopup() {
        showMultiChoiceDialog(mUsedLocaleNames!!, R.string.remove_language, R.string.remove, false) { checkedItems ->
            for (i in checkedItems.indices) {
                if (!checkedItems[i]) continue
                val subtypes = mRichImm!!.getEnabledSubtypesForLocale(mUsedLocaleValues!![i])
                for (subtype in subtypes) {
                    mRichImm!!.removeSubtype(subtype)
                }
            }
            activity.invalidateOptionsMenu()
            buildContent()
        }
    }

    private fun showMultiChoiceDialog(
        names: Array<CharSequence>,
        titleRes: Int,
        positiveButtonRes: Int,
        allowAllChecked: Boolean,
        listener: OnMultiChoiceDialogAcceptListener
    ) {
        val checkedItems = BooleanArray(names.size)
        mAlertDialog = AlertDialog.Builder(activity)
            .setTitle(titleRes)
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                var hasCheckedItem = false
                var hasUncheckedItem = false
                for (itemChecked in checkedItems) {
                    if (itemChecked) {
                        hasCheckedItem = true
                        if (allowAllChecked) break
                    } else {
                        hasUncheckedItem = true
                    }
                    if (hasCheckedItem && hasUncheckedItem) break
                }
                mAlertDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    hasCheckedItem && (hasUncheckedItem || allowAllChecked)
            }
            .setPositiveButton(positiveButtonRes) { _, _ ->
                listener.onClick(checkedItems)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
        mAlertDialog!!.show()
        mAlertDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    private interface OnMultiChoiceDialogAcceptListener {
        fun onClick(checkedItems: BooleanArray)
    }

    private class SingleLanguagePreference(context: Context, localeString: String) : Preference(context) {
        private val mLocale: String = localeString
        private var mExtras: Bundle? = null

        init {
            setTitle(LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString))
            fragment = SingleLanguageSettingsFragment::class.java.name
        }

        override fun getExtras(): Bundle {
            if (mExtras == null) {
                mExtras = Bundle()
                mExtras!!.putString(SingleLanguageSettingsFragment.LOCALE_BUNDLE_KEY, mLocale)
            }
            return mExtras!!
        }

        override fun peekExtras(): Bundle? = mExtras
    }

    companion object {
        private const val TAG = "LanguagesSettingsFragment"
        private const val DEBUG_SUBTYPE_ID = false
    }
}
