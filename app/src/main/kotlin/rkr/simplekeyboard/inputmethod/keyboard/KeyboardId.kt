/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2024 wittmane
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

import android.text.InputType
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import rkr.simplekeyboard.inputmethod.compat.EditorInfoCompatUtils
import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils
import java.util.Arrays
import java.util.Locale

/**
 * Unique identifier for each keyboard type.
 */
class KeyboardId(
    val mElementId: Int,
    params: KeyboardLayoutSet.Params
) {
    val mSubtype: Subtype = params.mSubtype
    val mThemeId: Int = params.mKeyboardThemeId
    val mWidth: Int = params.mKeyboardWidth
    val mHeight: Int = params.mKeyboardHeight
    val mBottomOffset: Int = params.mKeyboardBottomOffset
    val mMode: Int = params.mMode
    val mEditorInfo: EditorInfo = params.mEditorInfo
    val mClobberSettingsKey: Boolean = params.mNoSettingsKey
    val mLanguageSwitchKeyEnabled: Boolean = params.mLanguageSwitchKeyEnabled
    val mCustomActionLabel: String? = if (mEditorInfo.actionLabel != null) {
        mEditorInfo.actionLabel.toString()
    } else {
        null
    }
    val mShowMoreKeys: Boolean = params.mShowMoreKeys
    val mShowNumberRow: Boolean = params.mShowNumberRow

    private val mHashCode: Int = computeHashCode(this)

    private fun computeHashCode(id: KeyboardId): Int {
        return Arrays.hashCode(
            arrayOf(
                id.mElementId,
                id.mMode,
                id.mWidth,
                id.mHeight,
                id.mBottomOffset,
                id.passwordInput,
                id.mClobberSettingsKey,
                id.mLanguageSwitchKeyEnabled,
                id.isMultiLine,
                id.imeAction,
                id.mCustomActionLabel,
                id.navigateNext,
                id.navigatePrevious,
                id.mSubtype,
                id.mThemeId
            )
        )
    }

    private fun equalsInternal(other: KeyboardId): Boolean {
        if (other === this) return true
        return other.mElementId == mElementId &&
                other.mMode == mMode &&
                other.mWidth == mWidth &&
                other.mHeight == mHeight &&
                other.mBottomOffset == mBottomOffset &&
                other.passwordInput == passwordInput &&
                other.mClobberSettingsKey == mClobberSettingsKey &&
                other.mLanguageSwitchKeyEnabled == mLanguageSwitchKeyEnabled &&
                other.isMultiLine == isMultiLine &&
                other.imeAction == imeAction &&
                TextUtils.equals(other.mCustomActionLabel, mCustomActionLabel) &&
                other.navigateNext == navigateNext &&
                other.navigatePrevious == navigatePrevious &&
                other.mSubtype == mSubtype &&
                other.mThemeId == mThemeId
    }

    val isAlphabetKeyboard: Boolean
        get() = mElementId < ELEMENT_SYMBOLS

    val navigateNext: Boolean
        get() = (mEditorInfo.imeOptions and EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0 ||
                imeAction == EditorInfo.IME_ACTION_NEXT

    val navigatePrevious: Boolean
        get() = (mEditorInfo.imeOptions and EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS) != 0 ||
                imeAction == EditorInfo.IME_ACTION_PREVIOUS

    val passwordInput: Boolean
        get() {
            val inputType = mEditorInfo.inputType
            return InputTypeUtils.isPasswordInputType(inputType) ||
                    InputTypeUtils.isVisiblePasswordInputType(inputType)
        }

    val isMultiLine: Boolean
        get() = (mEditorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

    val imeAction: Int
        get() = InputTypeUtils.getImeOptionsActionIdFromEditorInfo(mEditorInfo)

    fun getLocale(): Locale = mSubtype.getLocaleObject()

    override fun equals(other: Any?): Boolean {
        return other is KeyboardId && equalsInternal(other)
    }

    override fun hashCode(): Int = mHashCode

    override fun toString(): String {
        return String.format(
            Locale.ROOT, "[%s %s:%s %dx%d +%d %s %s%s%s%s%s%s %s]",
            elementIdToName(mElementId),
            mSubtype.getLocale(),
            mSubtype.getKeyboardLayoutSet(),
            mWidth, mHeight, mBottomOffset,
            modeName(mMode),
            actionName(imeAction),
            if (navigateNext) " navigateNext" else "",
            if (navigatePrevious) " navigatePrevious" else "",
            if (mClobberSettingsKey) " clobberSettingsKey" else "",
            if (passwordInput) " passwordInput" else "",
            if (mLanguageSwitchKeyEnabled) " languageSwitchKeyEnabled" else "",
            if (isMultiLine) " isMultiLine" else "",
            KeyboardTheme.getKeyboardThemeName(mThemeId)
        )
    }

    companion object {
        const val MODE_TEXT = 0
        const val MODE_URL = 1
        const val MODE_EMAIL = 2
        const val MODE_IM = 3
        const val MODE_PHONE = 4
        const val MODE_NUMBER = 5
        const val MODE_DATE = 6
        const val MODE_TIME = 7
        const val MODE_DATETIME = 8

        const val ELEMENT_ALPHABET = 0
        const val ELEMENT_ALPHABET_MANUAL_SHIFTED = 1
        const val ELEMENT_ALPHABET_AUTOMATIC_SHIFTED = 2
        const val ELEMENT_ALPHABET_SHIFT_LOCKED = 3
        const val ELEMENT_SYMBOLS = 5
        const val ELEMENT_SYMBOLS_SHIFTED = 6
        const val ELEMENT_PHONE = 7
        const val ELEMENT_PHONE_SYMBOLS = 8
        const val ELEMENT_NUMBER = 9

        @JvmStatic
        fun equivalentEditorInfoForKeyboard(a: EditorInfo?, b: EditorInfo?): Boolean {
            if (a == null && b == null) return true
            if (a == null || b == null) return false
            return a.inputType == b.inputType &&
                    a.imeOptions == b.imeOptions &&
                    TextUtils.equals(a.privateImeOptions, b.privateImeOptions)
        }

        @JvmStatic
        fun elementIdToName(elementId: Int): String? {
            return when (elementId) {
                ELEMENT_ALPHABET -> "alphabet"
                ELEMENT_ALPHABET_MANUAL_SHIFTED -> "alphabetManualShifted"
                ELEMENT_ALPHABET_AUTOMATIC_SHIFTED -> "alphabetAutomaticShifted"
                ELEMENT_ALPHABET_SHIFT_LOCKED -> "alphabetShiftLocked"
                ELEMENT_SYMBOLS -> "symbols"
                ELEMENT_SYMBOLS_SHIFTED -> "symbolsShifted"
                ELEMENT_PHONE -> "phone"
                ELEMENT_PHONE_SYMBOLS -> "phoneSymbols"
                ELEMENT_NUMBER -> "number"
                else -> null
            }
        }

        @JvmStatic
        fun modeName(mode: Int): String? {
            return when (mode) {
                MODE_TEXT -> "text"
                MODE_URL -> "url"
                MODE_EMAIL -> "email"
                MODE_IM -> "im"
                MODE_PHONE -> "phone"
                MODE_NUMBER -> "number"
                MODE_DATE -> "date"
                MODE_TIME -> "time"
                MODE_DATETIME -> "datetime"
                else -> null
            }
        }

        @JvmStatic
        fun actionName(actionId: Int): String {
            return if (actionId == InputTypeUtils.IME_ACTION_CUSTOM_LABEL) "actionCustomLabel"
            else EditorInfoCompatUtils.imeActionName(actionId)
        }
    }
}
