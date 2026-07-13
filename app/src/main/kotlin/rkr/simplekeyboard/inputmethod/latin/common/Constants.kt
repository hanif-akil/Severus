/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2023 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2018 Raimondas
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

package rkr.simplekeyboard.inputmethod.latin.common

object Constants {

    object Color {
        /** The alpha value for fully opaque. */
        const val ALPHA_OPAQUE = 255
    }

    object TextUtils {
        /**
         * Capitalization mode for [android.text.TextUtils.getCapsMode]: don't capitalize
         * characters. This value may be used with
         * [android.text.TextUtils.CAP_MODE_CHARACTERS],
         * [android.text.TextUtils.CAP_MODE_WORDS], and
         * [android.text.TextUtils.CAP_MODE_SENTENCES].
         */
        const val CAP_MODE_OFF = 0
    }

    const val NOT_A_CODE = -1
    const val NOT_A_COORDINATE = -1

    // A hint on how many characters to cache from the TextView. A good value of this is given by
    // how many characters we need to be able to almost always find the caps mode.
    const val EDITOR_CONTENTS_CACHE_SIZE = 1024
    // How many characters we accept for the recapitalization functionality. This needs to be
    // large enough for all reasonable purposes, but avoid purposeful attacks. 100k sounds about
    // right for this.
    const val MAX_CHARACTERS_FOR_RECAPITALIZATION = 1024 * 100

    @JvmStatic
    fun isValidCoordinate(coordinate: Int): Boolean {
        // Detect NOT_A_COORDINATE, SUGGESTION_STRIP_COORDINATE,
        // and SPELL_CHECKER_COORDINATE.
        return coordinate >= 0
    }

    /**
     * Custom request code used in
     * [rkr.simplekeyboard.inputmethod.keyboard.KeyboardActionListener.onCustomRequest].
     */
    // The code to show input method picker.
    const val CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER = 1

    /**
     * Some common keys code. Must be positive.
     */
    const val CODE_ENTER = '\n'.code
    const val CODE_TAB = '\t'.code
    const val CODE_SPACE = ' '.code
    const val CODE_PERIOD = '.'.code
    const val CODE_COMMA = ','.code
    const val CODE_SINGLE_QUOTE = '\''.code
    const val CODE_DOUBLE_QUOTE = '"'.code
    const val CODE_BACKSLASH = '\\'.code
    const val CODE_VERTICAL_BAR = '|'.code
    const val CODE_PERCENT = '%'.code
    const val CODE_INVERTED_QUESTION_MARK = 0xBF // ¿
    const val CODE_INVERTED_EXCLAMATION_MARK = 0xA1 // ¡

    /**
     * Special keys code. Must be negative.
     * These should be aligned with constants in
     * [rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardCodesSet].
     */
    const val CODE_SHIFT = -1
    const val CODE_CAPSLOCK = -2
    const val CODE_SWITCH_ALPHA_SYMBOL = -3
    const val CODE_OUTPUT_TEXT = -4
    const val CODE_DELETE = -5
    const val CODE_SETTINGS = -6
    const val CODE_PASTE = -7
    const val CODE_ACTION_NEXT = -8
    const val CODE_ACTION_PREVIOUS = -9
    const val CODE_LANGUAGE_SWITCH = -10
    const val CODE_SHIFT_ENTER = -11
    const val CODE_SYMBOL_SHIFT = -12
    // Code value representing the code is not specified.
    const val CODE_UNSPECIFIED = -13
    const val CODE_CLIPBOARD = -14
    const val CODE_NUMPAD = -15
    const val CODE_COPY = -16
    const val CODE_CUT = -17

    @JvmStatic
    fun isLetterCode(code: Int): Boolean {
        return code >= CODE_SPACE
    }

    @JvmStatic
    fun printableCode(code: Int): String {
        return when (code) {
            CODE_SHIFT -> "shift"
            CODE_CAPSLOCK -> "capslock"
            CODE_SWITCH_ALPHA_SYMBOL -> "symbol"
            CODE_OUTPUT_TEXT -> "text"
            CODE_DELETE -> "delete"
            CODE_SETTINGS -> "settings"
            CODE_PASTE -> "paste"
            CODE_ACTION_NEXT -> "actionNext"
            CODE_ACTION_PREVIOUS -> "actionPrevious"
            CODE_LANGUAGE_SWITCH -> "languageSwitch"
            CODE_SHIFT_ENTER -> "shiftEnter"
            CODE_CLIPBOARD -> "clipboard"
            CODE_NUMPAD -> "numpad"
            CODE_COPY -> "copy"
            CODE_CUT -> "cut"
            CODE_UNSPECIFIED -> "unspec"
            CODE_TAB -> "tab"
            CODE_ENTER -> "enter"
            CODE_SPACE -> "space"
            else -> {
                when {
                    code < CODE_SPACE -> String.format("\\u%02X", code)
                    code < 0x100 -> String.format("%c", code)
                    code < 0x10000 -> String.format("\\u%04X", code)
                    else -> String.format("\\U%05X", code)
                }
            }
        }
    }

    /**
     * Screen metrics (a.k.a. Device form factor) constants of
     * [rkr.simplekeyboard.inputmethod.R.integer.config_screen_metrics].
     */
    const val SCREEN_METRICS_LARGE_TABLET = 2
    const val SCREEN_METRICS_SMALL_TABLET = 3
}
