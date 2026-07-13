/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2020 Raimondas Rimkus
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
import android.preference.PreferenceFragment
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat

abstract class InputMethodSettingsFragment : PreferenceFragment() {
    private val mSettings = InputMethodSettingsImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = activity
        preferenceScreen = preferenceManager.createPreferenceScreen(
            PreferenceManagerCompat.getDeviceContext(context)
        )
        mSettings.init(context, preferenceScreen)
    }

    override fun onResume() {
        super.onResume()
        mSettings.updateEnabledSubtypeList()
    }
}
