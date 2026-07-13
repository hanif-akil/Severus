/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
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

import android.content.res.Resources
import java.util.Arrays
import java.util.Locale
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils

class SpacingAndPunctuations(res: Resources) {
    val mSortedWordSeparators: IntArray = StringUtils.toSortedCodePointArray(
        res.getString(R.string.symbols_word_separators)
    )
    private val mSentenceSeparator: Int = res.getInteger(R.integer.sentence_separator)
    private val mAbbreviationMarker: Int = res.getInteger(R.integer.abbreviation_marker)
    private val mSortedSentenceTerminators: IntArray = StringUtils.toSortedCodePointArray(
        res.getString(R.string.symbols_sentence_terminators)
    )
    val mUsesAmericanTypography: Boolean =
        Locale.ENGLISH.language == res.configuration.locale.language
    val mUsesGermanRules: Boolean =
        Locale.GERMAN.language == res.configuration.locale.language

    fun isWordSeparator(code: Int): Boolean {
        return Arrays.binarySearch(mSortedWordSeparators, code) >= 0
    }

    fun isSentenceTerminator(code: Int): Boolean {
        return Arrays.binarySearch(mSortedSentenceTerminators, code) >= 0
    }

    fun isAbbreviationMarker(code: Int): Boolean {
        return code == mAbbreviationMarker
    }

    fun isSentenceSeparator(code: Int): Boolean {
        return code == mSentenceSeparator
    }
}
