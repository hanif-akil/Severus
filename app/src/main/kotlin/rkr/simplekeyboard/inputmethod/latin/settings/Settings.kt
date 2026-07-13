/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
 * Copyright (C) 2019 Micha LaQua
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme
import rkr.simplekeyboard.inputmethod.latin.AudioAndHapticFeedbackManager
import rkr.simplekeyboard.inputmethod.latin.InputAttributes
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager
import java.util.concurrent.locks.ReentrantLock

class Settings private constructor() : BroadcastReceiver(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var mContext: Context? = null
    private var mRes: android.content.res.Resources? = null
    private var mPrefs: SharedPreferences? = null
    private var mSettingsValues: SettingsValues? = null
    private var mRestrictionsMgr: RestrictionsManager? = null
    private val mSettingsValuesLock = ReentrantLock()

    fun onCreate(context: Context) {
        mContext = context
        mRes = context.resources
        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context)
        mPrefs!!.registerOnSharedPreferenceChangeListener(this)
        mRestrictionsMgr = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager?
        loadRestrictions(mRestrictionsMgr!!, mPrefs!!)
        context.registerReceiver(this, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED))
    }

    fun onDestroy() {
        mPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
        mContext!!.unregisterReceiver(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        mSettingsValuesLock.lock()
        try {
            if (mSettingsValues == null) {
                Log.w(TAG, "onSharedPreferenceChanged called before loadSettings.")
                return
            }
            loadSettings(mSettingsValues!!.mInputAttributes)
        } finally {
            mSettingsValuesLock.unlock()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        loadRestrictions(mRestrictionsMgr!!, mPrefs!!)
        onSharedPreferenceChanged(mPrefs!!, null)
        RichInputMethodManager.getInstance().reloadSubtypes(context)
    }

    fun loadSettings(inputAttributes: InputAttributes) {
        mSettingsValues = SettingsValues(mPrefs!!, mRes!!, inputAttributes)
    }

    fun getCurrent(): SettingsValues? {
        return mSettingsValues
    }

    companion object {
        private const val TAG = "Settings"
        const val ACTIVE_RESTRICTIONS = "active_restrictions"
        const val SCREEN_THEME = "screen_theme"
        const val PREF_AUTO_CAP = "auto_cap"
        const val PREF_VIBRATE_ON = "vibrate_on"
        const val PREF_SOUND_ON = "sound_on"
        const val PREF_POPUP_ON = "popup_on"
        const val PREF_SHOW_LANGUAGE_SWITCH_KEY = "pref_show_language_switch_key"
        const val PREF_USE_ON_SCREEN = "pref_use_on_screen"
        const val PREF_ENABLE_IME_SWITCH = "pref_enable_ime_switch"
        const val PREF_ENABLED_SUBTYPES = "pref_enabled_subtypes"
        const val PREF_KEYPRESS_SOUND_VOLUME = "pref_keypress_sound_volume"
        const val PREF_KEY_LONGPRESS_TIMEOUT = "pref_key_longpress_timeout"
        const val PREF_KEYBOARD_HEIGHT = "pref_keyboard_height"
        const val PREF_BOTTOM_OFFSET_PORTRAIT = "pref_bottom_offset_portrait"
        const val PREF_KEYBOARD_COLOR = "pref_keyboard_color"
        const val PREF_SHOW_SPECIAL_CHARS = "pref_show_special_chars"
        const val PREF_SHOW_NUMBER_ROW = "pref_show_number_row"
        const val PREF_SPACE_SWIPE = "pref_space_swipe"
        const val PREF_DELETE_SWIPE = "pref_delete_swipe"
        const val PREF_CLIPBOARD_MAX_ITEMS = "pref_clipboard_max_items"
        const val PREF_SHOW_TOOLBAR = "pref_show_toolbar"
        const val PREF_LONG_PRESS_CVX = "pref_long_press_cvx"

        private const val UNDEFINED_PREFERENCE_VALUE_FLOAT = -1.0f
        private const val UNDEFINED_PREFERENCE_VALUE_INT = -1
        private const val DEFAULT_KEYPRESS_SOUND_VOLUME = 0.5f
        const val DEFAULT_BOTTOM_OFFSET = 0

        private val sInstance = Settings()

        @JvmStatic
        fun getInstance(): Settings = sInstance

        @JvmStatic
        fun init(context: Context) {
            sInstance.onCreate(context)
        }

        @JvmStatic
        fun loadRestrictions(
            restrictionsMgr: RestrictionsManager,
            prefs: SharedPreferences
        ): Set<String> {
            val appRestrictions = restrictionsMgr.applicationRestrictions
            val restrictionKeys = appRestrictions.keySet()
            if (restrictionKeys.isEmpty()) {
                if (prefs.contains(ACTIVE_RESTRICTIONS)) {
                    prefs.edit().remove(ACTIVE_RESTRICTIONS).apply()
                }
            } else {
                val prefsEditor = prefs.edit()
                for (key in restrictionKeys) {
                    when (key) {
                        PREF_ENABLED_SUBTYPES -> {
                            Log.i(TAG, "Loading restriction: $key=${appRestrictions.getString(key)}")
                            prefsEditor.putString(key, appRestrictions.getString(key))
                        }
                        SCREEN_THEME -> {
                            Log.i(TAG, "Loading restriction: $key=${appRestrictions.getString(key)}")
                            prefsEditor.putString(KeyboardTheme.KEYBOARD_THEME_KEY, appRestrictions.getString(key))
                        }
                        PREF_AUTO_CAP, PREF_SHOW_NUMBER_ROW, PREF_SHOW_SPECIAL_CHARS,
                        PREF_SHOW_LANGUAGE_SWITCH_KEY, PREF_USE_ON_SCREEN, PREF_ENABLE_IME_SWITCH,
                        PREF_DELETE_SWIPE, PREF_SPACE_SWIPE, PREF_LONG_PRESS_CVX,
                        PREF_VIBRATE_ON, PREF_SOUND_ON, PREF_POPUP_ON -> {
                            Log.i(TAG, "Loading restriction: $key=${appRestrictions.getBoolean(key)}")
                            prefsEditor.putBoolean(key, appRestrictions.getBoolean(key))
                        }
                        PREF_KEYPRESS_SOUND_VOLUME, PREF_KEYBOARD_HEIGHT -> {
                            Log.i(TAG, "Loading restriction: $key=${appRestrictions.getInt(key)}")
                            prefsEditor.putFloat(key, appRestrictions.getInt(key) / 100f)
                        }
                        PREF_KEY_LONGPRESS_TIMEOUT, PREF_BOTTOM_OFFSET_PORTRAIT -> {
                            Log.i(TAG, "Loading restriction: $key=${appRestrictions.getInt(key)}")
                            prefsEditor.putInt(key, appRestrictions.getInt(key))
                        }
                        PREF_KEYBOARD_COLOR -> {
                            Log.i(TAG, "Loading restriction: $key=${appRestrictions.getString(key)}")
                            var color = appRestrictions.getString(key)
                            if (color != null && color.startsWith("#")) {
                                try {
                                    color = "FF" + color.substring(1)
                                    prefsEditor.putInt(key, color.toLong(16).toInt())
                                    continue@for
                                } catch (ignored: NumberFormatException) { }
                            }
                            prefsEditor.remove(key)
                        }
                        else -> Log.e(TAG, "Unhandled restriction: $key")
                    }
                }
                prefsEditor.putStringSet(ACTIVE_RESTRICTIONS, restrictionKeys)
                prefsEditor.apply()
            }
            return restrictionKeys
        }

        @JvmStatic
        fun readKeypressSoundEnabled(prefs: SharedPreferences, res: android.content.res.Resources): Boolean {
            return prefs.getBoolean(PREF_SOUND_ON, res.getBoolean(R.bool.config_default_sound_enabled))
        }

        @JvmStatic
        fun readVibrationEnabled(prefs: SharedPreferences, res: android.content.res.Resources): Boolean {
            val hasVibrator = AudioAndHapticFeedbackManager.getInstance().hasVibrator()
            return hasVibrator && prefs.getBoolean(PREF_VIBRATE_ON, res.getBoolean(R.bool.config_default_vibration_enabled))
        }

        @JvmStatic
        fun readKeyPreviewPopupEnabled(prefs: SharedPreferences, res: android.content.res.Resources): Boolean {
            val defaultKeyPreviewPopup = res.getBoolean(R.bool.config_default_key_preview_popup)
            return prefs.getBoolean(PREF_POPUP_ON, defaultKeyPreviewPopup)
        }

        @JvmStatic
        fun readShowLanguageSwitchKey(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SHOW_LANGUAGE_SWITCH_KEY, true)
        }

        @JvmStatic
        fun readUseOnScreenKeyboard(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_USE_ON_SCREEN, false)
        }

        @JvmStatic
        fun readEnableImeSwitch(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_ENABLE_IME_SWITCH, false)
        }

        @JvmStatic
        fun readShowSpecialChars(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SHOW_SPECIAL_CHARS, true)
        }

        @JvmStatic
        fun readShowNumberRow(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SHOW_NUMBER_ROW, false)
        }

        @JvmStatic
        fun readSpaceSwipeEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SPACE_SWIPE, false)
        }

        @JvmStatic
        fun readDeleteSwipeEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_DELETE_SWIPE, false)
        }

        @JvmStatic
        fun readClipboardMaxItems(prefs: SharedPreferences): Int {
            return prefs.getInt(PREF_CLIPBOARD_MAX_ITEMS, 10)
        }

        @JvmStatic
        fun readShowToolbar(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SHOW_TOOLBAR, true)
        }

        @JvmStatic
        fun readLongPressCvxEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_LONG_PRESS_CVX, false)
        }

        @JvmStatic
        fun readPrefSubtypes(prefs: SharedPreferences): String? {
            return prefs.getString(PREF_ENABLED_SUBTYPES, "")
        }

        @JvmStatic
        fun writePrefSubtypes(prefs: SharedPreferences, prefSubtypes: String?) {
            prefs.edit().putString(PREF_ENABLED_SUBTYPES, prefSubtypes).apply()
        }

        @JvmStatic
        fun readKeypressSoundVolume(prefs: SharedPreferences): Float {
            val volume = prefs.getFloat(PREF_KEYPRESS_SOUND_VOLUME, UNDEFINED_PREFERENCE_VALUE_FLOAT)
            return if (volume != UNDEFINED_PREFERENCE_VALUE_FLOAT) volume else readDefaultKeypressSoundVolume()
        }

        @JvmStatic
        fun readDefaultKeypressSoundVolume(): Float = DEFAULT_KEYPRESS_SOUND_VOLUME

        @JvmStatic
        fun readKeyLongpressTimeout(prefs: SharedPreferences, res: android.content.res.Resources): Int {
            val milliseconds = prefs.getInt(PREF_KEY_LONGPRESS_TIMEOUT, UNDEFINED_PREFERENCE_VALUE_INT)
            return if (milliseconds != UNDEFINED_PREFERENCE_VALUE_INT) milliseconds else readDefaultKeyLongpressTimeout(res)
        }

        @JvmStatic
        fun readDefaultKeyLongpressTimeout(res: android.content.res.Resources): Int {
            return res.getInteger(R.integer.config_default_longpress_key_timeout)
        }

        @JvmStatic
        fun readKeyboardHeight(prefs: SharedPreferences, defaultValue: Float): Float {
            return prefs.getFloat(PREF_KEYBOARD_HEIGHT, defaultValue)
        }

        @JvmStatic
        fun readBottomOffsetPortrait(prefs: SharedPreferences): Int {
            return prefs.getInt(PREF_BOTTOM_OFFSET_PORTRAIT, DEFAULT_BOTTOM_OFFSET)
        }

        @JvmStatic
        fun readKeyboardDefaultColor(context: Context): Int {
            val keyboardThemeColors = context.resources.getIntArray(R.array.keyboard_theme_colors)
            val keyboardThemeIds = context.resources.getIntArray(R.array.keyboard_theme_ids)
            val themeId = getKeyboardTheme(context).mThemeId
            for (index in keyboardThemeIds.indices) {
                if (themeId == keyboardThemeIds[index]) {
                    return keyboardThemeColors[index]
                }
            }
            return Color.TRANSPARENT
        }

        @JvmStatic
        fun getKeyboardTheme(context: Context): KeyboardTheme {
            return KeyboardTheme.getKeyboardTheme(context)
        }

        @JvmStatic
        fun readKeyboardColor(prefs: SharedPreferences, context: Context): Int {
            return prefs.getInt(PREF_KEYBOARD_COLOR, readKeyboardDefaultColor(context))
        }

        @JvmStatic
        fun removeKeyboardColor(prefs: SharedPreferences) {
            prefs.edit().remove(PREF_KEYBOARD_COLOR).apply()
        }

        @JvmStatic
        fun readUseFullscreenMode(res: android.content.res.Resources): Boolean {
            return res.getBoolean(R.bool.config_use_fullscreen_mode)
        }

        @JvmStatic
        fun readHasHardwareKeyboard(conf: Configuration): Boolean {
            return conf.keyboard != Configuration.KEYBOARD_NOKEYS
                    && conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
        }
    }
}
