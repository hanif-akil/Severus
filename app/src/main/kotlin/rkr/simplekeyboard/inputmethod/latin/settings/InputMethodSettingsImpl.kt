/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2017 Raimondas Rimkus
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
import android.content.RestrictionsManager
import android.preference.Preference
import android.preference.PreferenceScreen
import android.text.TextUtils
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import rkr.simplekeyboard.inputmethod.latin.Subtype

internal class InputMethodSettingsImpl {
    private var mSubtypeEnablerPreference: Preference? = null
    private var mRichImm: RichInputMethodManager? = null

    fun init(context: Context, prefScreen: PreferenceScreen): Boolean {
        RichInputMethodManager.init(context)
        mRichImm = RichInputMethodManager.getInstance()

        val prefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
        val restrictionsMgr = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictionKeys = Settings.loadRestrictions(restrictionsMgr, prefs)

        mSubtypeEnablerPreference = Preference(context).apply {
            setTitle(R.string.select_language)
            fragment = LanguagesSettingsFragment::class.java.name
            isEnabled = !restrictionKeys.contains(Settings.PREF_ENABLED_SUBTYPES)
        }
        prefScreen.addPreference(mSubtypeEnablerPreference)
        updateEnabledSubtypeList()
        return true
    }

    fun updateEnabledSubtypeList() {
        mSubtypeEnablerPreference?.let { pref ->
            val summary = getEnabledSubtypesLabel(mRichImm)
            if (!TextUtils.isEmpty(summary)) {
                pref.summary = summary
            }
        }
    }

    companion object {
        private fun getEnabledSubtypesLabel(richImm: RichInputMethodManager?): String? {
            richImm ?: return null
            val subtypes = richImm.getEnabledSubtypes(true)
            val sb = StringBuilder()
            for (subtype in subtypes) {
                if (sb.length > 0) {
                    sb.append(", ")
                }
                sb.append(subtype.name)
            }
            return sb.toString()
        }
    }
}
