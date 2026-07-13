/*
 * Copyright (C) 2014 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.AudioAndHapticFeedbackManager

class KeyPressSettingsFragment : SubScreenFragment() {

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_key_press)

        val context = activity
        AudioAndHapticFeedbackManager.init(context)

        if (!AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
            removePreference(Settings.PREF_VIBRATE_ON)
        }

        setupKeypressSoundVolumeSettings()
        setupKeyLongpressTimeoutSettings()
    }

    private fun setupKeypressSoundVolumeSettings() {
        val pref = findPreference(Settings.PREF_KEYPRESS_SOUND_VOLUME) as? SeekBarDialogPreference
            ?: return
        val prefs = sharedPreferences
        val res = resources
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            private val PERCENTAGE_FLOAT = 100.0f
            private fun getValueFromPercentage(percentage: Int): Float = percentage / PERCENTAGE_FLOAT
            private fun getPercentageFromValue(floatValue: Float): Int = (floatValue * PERCENTAGE_FLOAT).toInt()

            override fun writeValue(value: Int, key: String) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply()
            }

            override fun writeDefaultValue(key: String) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String): Int = getPercentageFromValue(Settings.readKeypressSoundVolume(prefs))

            override fun readDefaultValue(key: String): Int = getPercentageFromValue(Settings.readDefaultKeypressSoundVolume())

            override fun getValueText(value: Int): String {
                return if (value < 0) res.getString(R.string.settings_system_default) else value.toString()
            }

            override fun feedbackValue(value: Int) {
                AudioAndHapticFeedbackManager.getInstance().playSoundEffect(
                    AudioManager.FX_KEYPRESS_STANDARD, getValueFromPercentage(value)
                )
            }
        })
    }

    private fun setupKeyLongpressTimeoutSettings() {
        val prefs = sharedPreferences
        val res = resources
        val pref = findPreference(Settings.PREF_KEY_LONGPRESS_TIMEOUT) as? SeekBarDialogPreference
            ?: return
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String) {
                prefs.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String): Int = Settings.readKeyLongpressTimeout(prefs, res)

            override fun readDefaultValue(key: String): Int = Settings.readDefaultKeyLongpressTimeout(res)

            override fun getValueText(value: Int): String = res.getString(R.string.abbreviation_unit_milliseconds, value)

            override fun feedbackValue(value: Int) {}
        })
    }
}
