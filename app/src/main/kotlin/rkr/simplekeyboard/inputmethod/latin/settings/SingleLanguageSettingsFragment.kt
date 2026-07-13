/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2023 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.Context
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.preference.SwitchPreference
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils

class SingleLanguageSettingsFragment : PreferenceFragment() {

    private var mRichImm: RichInputMethodManager? = null
    private var mSubtypePreferences: MutableList<SubtypePreference>? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        RichInputMethodManager.init(activity)
        mRichImm = RichInputMethodManager.getInstance()
        addPreferencesFromResource(R.xml.empty_settings)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val context = activity

        val args = arguments
        if (args != null) {
            val locale = arguments.getString(LOCALE_BUNDLE_KEY)
            buildContent(locale, context)
        }

        super.onActivityCreated(savedInstanceState)
    }

    private fun buildContent(locale: String?, context: Context) {
        locale ?: return
        val group = preferenceScreen
        group.removeAll()

        val mainCategory = PreferenceCategory(context)
        val localeName = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(locale)
        mainCategory.title = context.getString(R.string.generic_language_layouts, localeName)
        group.addPreference(mainCategory)

        buildSubtypePreferences(locale, group, context)
    }

    private fun buildSubtypePreferences(locale: String, group: PreferenceGroup, context: Context) {
        val enabledSubtypes = mRichImm!!.getEnabledSubtypes(false)
        val subtypes = SubtypeLocaleUtils.getSubtypes(locale, context.resources)
        mSubtypePreferences = mutableListOf()
        for (subtype in subtypes) {
            val isChecked = enabledSubtypes.contains(subtype)
            val pref = createSubtypePreference(subtype, isChecked, context)
            group.addPreference(pref)
            mSubtypePreferences!!.add(pref)
        }

        val checkedPrefs = getCheckedSubtypePreferences()
        if (checkedPrefs.size == 1) {
            checkedPrefs[0].isEnabled = false
        }
    }

    private fun createSubtypePreference(
        subtype: Subtype,
        checked: Boolean,
        context: Context
    ): SubtypePreference {
        val pref = SubtypePreference(context, subtype)
        pref.title = subtype.layoutDisplayName
        pref.isChecked = checked

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue !is Boolean) return@OnPreferenceChangeListener false
            val isEnabling = newValue
            val pref = preference as SubtypePreference
            val checkedPrefs = getCheckedSubtypePreferences()
            if (checkedPrefs.size == 1) {
                checkedPrefs[0].isEnabled = false
            }
            if (isEnabling) {
                val added = mRichImm!!.addSubtype(pref.subtype)
                if (added && checkedPrefs.size == 1) {
                    checkedPrefs[0].isEnabled = true
                }
                added
            } else {
                val removed = mRichImm!!.removeSubtype(pref.subtype)
                if (removed && checkedPrefs.size == 2) {
                    val onlyCheckedPref = if (checkedPrefs[0] == pref) checkedPrefs[1] else checkedPrefs[0]
                    onlyCheckedPref.isEnabled = false
                }
                removed
            }
        }

        return pref
    }

    private fun getCheckedSubtypePreferences(): List<SubtypePreference> {
        val prefs = mutableListOf<SubtypePreference>()
        for (pref in mSubtypePreferences!!) {
            if (pref.isChecked) {
                prefs.add(pref)
            }
        }
        return prefs
    }

    private class SubtypePreference(context: Context, subtype: Subtype) : SwitchPreference(context) {
        val mSubtype: Subtype = subtype

        val subtype: Subtype
            get() = mSubtype
    }

    companion object {
        const val LOCALE_BUNDLE_KEY = "LOCALE"
    }
}
