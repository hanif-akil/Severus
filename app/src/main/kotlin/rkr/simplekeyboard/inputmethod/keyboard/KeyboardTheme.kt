/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2024 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.latin.settings.Settings

class KeyboardTheme private constructor(
    val mThemeId: Int,
    val mThemeName: String,
    val mStyleId: Int,
    val mCustomColorSupport: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is KeyboardTheme && other.mThemeId == mThemeId
    }

    override fun hashCode(): Int = mThemeId

    companion object {
        private const val TAG = "KeyboardTheme"

        @JvmField
        const val KEYBOARD_THEME_KEY = "pref_keyboard_theme_20140509"

        const val THEME_ID_LIGHT_BORDER = 1
        const val THEME_ID_DARK_BORDER = 2
        const val THEME_ID_LIGHT = 3
        const val THEME_ID_DARK = 4
        const val THEME_ID_SYSTEM = 5
        const val THEME_ID_SYSTEM_BORDER = 6
        const val DEFAULT_THEME_ID = THEME_ID_SYSTEM

        internal val KEYBOARD_THEMES = arrayOf(
            KeyboardTheme(THEME_ID_SYSTEM, "LXXSystem", R.style.KeyboardTheme_LXX_System, false),
            KeyboardTheme(THEME_ID_SYSTEM_BORDER, "LXXSystemBorder", R.style.KeyboardTheme_LXX_System_Border, false),
            KeyboardTheme(THEME_ID_LIGHT, "LXXLight", R.style.KeyboardTheme_LXX_Light, true),
            KeyboardTheme(THEME_ID_DARK, "LXXDark", R.style.KeyboardTheme_LXX_Dark, true),
            KeyboardTheme(THEME_ID_LIGHT_BORDER, "LXXLightBorder", R.style.KeyboardTheme_LXX_Light_Border, true),
            KeyboardTheme(THEME_ID_DARK_BORDER, "LXXDarkBorder", R.style.KeyboardTheme_LXX_Dark_Border, true),
        )

        internal fun searchKeyboardThemeById(themeId: Int): KeyboardTheme? {
            for (theme in KEYBOARD_THEMES) {
                if (theme.mThemeId == themeId) {
                    return theme
                }
            }
            return null
        }

        internal fun getDefaultKeyboardTheme(): KeyboardTheme? {
            return searchKeyboardThemeById(DEFAULT_THEME_ID)
        }

        @JvmStatic
        fun getKeyboardThemeName(themeId: Int): String {
            val theme = searchKeyboardThemeById(themeId)
            return theme?.mThemeName ?: ""
        }

        @JvmStatic
        fun saveKeyboardThemeId(themeId: Int, prefs: SharedPreferences) {
            prefs.edit().putString(KEYBOARD_THEME_KEY, Integer.toString(themeId)).apply()
        }

        @JvmStatic
        fun getKeyboardTheme(context: Context): KeyboardTheme? {
            val prefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
            return getKeyboardTheme(prefs)
        }

        @JvmStatic
        fun getKeyboardTheme(prefs: SharedPreferences): KeyboardTheme? {
            val themeIdString = prefs.getString(KEYBOARD_THEME_KEY, null)
            if (themeIdString == null) {
                return getDefaultKeyboardTheme()
            }
            try {
                val themeId = themeIdString.toInt()
                val theme = searchKeyboardThemeById(themeId)
                if (theme != null) {
                    return theme
                }
                Log.w(TAG, "Unknown keyboard theme in preference: $themeIdString")
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Illegal keyboard theme in preference: $themeIdString", e)
            }
            prefs.edit().remove(KEYBOARD_THEME_KEY).remove(Settings.PREF_KEYBOARD_COLOR).apply()
            return getDefaultKeyboardTheme()
        }
    }
}
