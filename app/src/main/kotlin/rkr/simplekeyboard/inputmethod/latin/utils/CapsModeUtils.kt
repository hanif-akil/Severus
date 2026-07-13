/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2020 wittmane
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

package rkr.simplekeyboard.inputmethod.latin.utils

import android.text.InputType
import android.text.TextUtils
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.settings.SpacingAndPunctuations

object CapsModeUtils {

    private fun isStartPunctuation(codePoint: Int): Boolean {
        return (codePoint == Constants.CODE_DOUBLE_QUOTE || codePoint == Constants.CODE_SINGLE_QUOTE
                || codePoint == Constants.CODE_INVERTED_QUESTION_MARK
                || codePoint == Constants.CODE_INVERTED_EXCLAMATION_MARK
                || Character.getType(codePoint) == Character.START_PUNCTUATION)
    }

    @JvmStatic
    fun getCapsMode(
        cs: CharSequence, reqModes: Int,
        spacingAndPunctuations: SpacingAndPunctuations
    ): Int {
        if (reqModes and (TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES) == 0) {
            return TextUtils.CAP_MODE_CHARACTERS and reqModes
        }

        var i: Int
        i = cs.length
        while (i > 0) {
            val c = cs[i - 1]
            if (!isStartPunctuation(c)) {
                break
            }
            i--
        }
        val newCapIndex = i

        var prevChar = Constants.CODE_SPACE
        while (i > 0) {
            prevChar = cs[i - 1]
            if (!Character.isSpaceChar(prevChar) && prevChar != Constants.CODE_TAB) {
                break
            }
            i--
        }
        if (i <= 0 || Character.isWhitespace(prevChar)) {
            if (spacingAndPunctuations.mUsesGermanRules) {
                var hasNewLine = false
                while (--i >= 0 && Character.isWhitespace(prevChar)) {
                    if (Constants.CODE_ENTER == prevChar) {
                        hasNewLine = true
                    }
                    prevChar = cs[i]
                }
                if (Constants.CODE_COMMA == prevChar && hasNewLine) {
                    return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
                }
            }
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS
                    or TextUtils.CAP_MODE_SENTENCES) and reqModes
        }
        if (newCapIndex == i) {
            if (spacingAndPunctuations.isWordSeparator(cs[cs.length - 1])) {
                return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
            }
            return TextUtils.CAP_MODE_CHARACTERS and reqModes
        }
        if (reqModes and TextUtils.CAP_MODE_SENTENCES == 0) {
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
        }

        if (spacingAndPunctuations.mUsesAmericanTypography) {
            while (i > 0) {
                val c = cs[i - 1]
                if (c != Constants.CODE_DOUBLE_QUOTE && c != Constants.CODE_SINGLE_QUOTE
                    && Character.getType(c) != Character.END_PUNCTUATION
                ) {
                    break
                }
                i--
            }
        }

        if (i <= 0) {
            return TextUtils.CAP_MODE_CHARACTERS and reqModes
        }
        var c = cs[--i]

        if (spacingAndPunctuations.isSentenceTerminator(c)
            && !spacingAndPunctuations.isAbbreviationMarker(c)
        ) {
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS
                    or TextUtils.CAP_MODE_SENTENCES) and reqModes
        }
        if (!spacingAndPunctuations.isSentenceSeparator(c) || i <= 0) {
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
        }

        val START = 0
        val WORD = 1
        val PERIOD = 2
        val LETTER = 3
        val NUMBER = 4
        val caps = (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS
                or TextUtils.CAP_MODE_SENTENCES) and reqModes
        val noCaps = (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
        var state = START
        while (i > 0) {
            c = cs[--i]
            when (state) {
                START -> {
                    if (Character.isLetter(c)) {
                        state = WORD
                    } else if (Character.isWhitespace(c)) {
                        return noCaps
                    } else if (Character.isDigit(c) && spacingAndPunctuations.mUsesGermanRules) {
                        state = NUMBER
                    } else {
                        return caps
                    }
                }
                WORD -> {
                    if (Character.isLetter(c)) {
                        state = WORD
                    } else if (spacingAndPunctuations.isSentenceSeparator(c)) {
                        state = PERIOD
                    } else {
                        return caps
                    }
                }
                PERIOD -> {
                    if (Character.isLetter(c)) {
                        state = LETTER
                    } else {
                        return caps
                    }
                }
                LETTER -> {
                    if (Character.isLetter(c)) {
                        state = LETTER
                    } else if (spacingAndPunctuations.isSentenceSeparator(c)) {
                        state = PERIOD
                    } else {
                        return noCaps
                    }
                }
                NUMBER -> {
                    if (Character.isLetter(c)) {
                        state = WORD
                    } else if (Character.isDigit(c)) {
                        state = NUMBER
                    } else {
                        return noCaps
                    }
                }
            }
        }
        return if (START == state || LETTER == state) noCaps else caps
    }

    @JvmStatic
    fun flagsToString(capsFlags: Int): String {
        val capsFlagsMask = TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS
                or TextUtils.CAP_MODE_SENTENCES
        if (capsFlags and capsFlagsMask.inv() != 0) {
            return "unknown<0x${Integer.toHexString(capsFlags)}>"
        }
        val builder = ArrayList<String>()
        if (capsFlags and TextUtils.CAP_MODE_CHARACTERS != 0) {
            builder.add("characters")
        }
        if (capsFlags and TextUtils.CAP_MODE_WORDS != 0) {
            builder.add("words")
        }
        if (capsFlags and TextUtils.CAP_MODE_SENTENCES != 0) {
            builder.add("sentences")
        }
        return if (builder.isEmpty()) "none" else TextUtils.join("|", builder)
    }
}
