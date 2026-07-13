/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.InputAttributes

// Non-final for testing via mock library.
open class SettingsValues(
    prefs: SharedPreferences,
    res: Resources,
    inputAttributes: InputAttributes
) {
    // From resources:
    val mSpacingAndPunctuations: SpacingAndPunctuations = SpacingAndPunctuations(res)
    // From configuration:
    val mHasHardwareKeyboard: Boolean = Settings.readHasHardwareKeyboard(res.configuration)
    val mDisplayOrientation: Int = res.configuration.orientation
    // From preferences, in the same order as xml/prefs.xml:
    val mAutoCap: Boolean = prefs.getBoolean(Settings.PREF_AUTO_CAP, true)
    val mVibrateOn: Boolean = Settings.readVibrationEnabled(prefs, res)
    val mSoundOn: Boolean = Settings.readKeypressSoundEnabled(prefs, res)
    val mKeyPreviewPopupOn: Boolean = Settings.readKeyPreviewPopupEnabled(prefs, res)
    val mUseOnScreen: Boolean = Settings.readUseOnScreenKeyboard(prefs)
    val mShowsLanguageSwitchKey: Boolean = Settings.readShowLanguageSwitchKey(prefs)
    val mImeSwitchEnabled: Boolean = Settings.readEnableImeSwitch(prefs)
    val mKeyLongpressTimeout: Int = Settings.readKeyLongpressTimeout(prefs, res)
    val mShowSpecialChars: Boolean = Settings.readShowSpecialChars(prefs)
    val mShowNumberRow: Boolean = Settings.readShowNumberRow(prefs)
    val mSpaceSwipeEnabled: Boolean = Settings.readSpaceSwipeEnabled(prefs)
    val mDeleteSwipeEnabled: Boolean = Settings.readDeleteSwipeEnabled(prefs)
    val mClipboardMaxItems: Int = Settings.readClipboardMaxItems(prefs)
    val mShowToolbar: Boolean = Settings.readShowToolbar(prefs)
    val mLongPressCvxEnabled: Boolean = Settings.readLongPressCvxEnabled(prefs)

    // From the input box
    val mInputAttributes: InputAttributes = inputAttributes

    // Deduced settings
    val mKeypressSoundVolume: Float = Settings.readKeypressSoundVolume(prefs)
    val mKeyPreviewPopupDismissDelay: Int =
        res.getInteger(R.integer.config_key_preview_linger_timeout)

    // Debug settings
    val mKeyboardHeightScale: Float = Settings.readKeyboardHeight(prefs, DEFAULT_SIZE_SCALE)
    val mBottomOffsetPortrait: Int = Settings.readBottomOffsetPortrait(prefs)

    fun isWordSeparator(code: Int): Boolean {
        return mSpacingAndPunctuations.isWordSeparator(code)
    }

    fun isLanguageSwitchKeyDisabled(): Boolean {
        return !mShowsLanguageSwitchKey
    }

    fun isSameInputType(editorInfo: EditorInfo): Boolean {
        return mInputAttributes.isSameInputType(editorInfo)
    }

    fun hasSameOrientation(configuration: Configuration): Boolean {
        return mDisplayOrientation == configuration.orientation
    }

    companion object {
        const val DEFAULT_SIZE_SCALE = 1.0f // 100%
    }
}
