/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
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

import android.text.TextUtils
import java.util.Arrays
import java.util.Locale

object StringUtils {

    @JvmStatic
    fun codePointCount(text: CharSequence?): Int {
        if (TextUtils.isEmpty(text)) {
            return 0
        }
        return Character.codePointCount(text, 0, text!!.length)
    }

    @JvmStatic
    fun newSingleCodePointString(codePoint: Int): String {
        if (Character.charCount(codePoint) == 1) {
            // Optimization: avoid creating a temporary array for characters that are
            // represented by a single char value
            return codePoint.toChar().toString()
        }
        // For surrogate pair
        return String(Character.toChars(codePoint))
    }

    @JvmStatic
    fun containsInArray(text: String, array: Array<String>): Boolean {
        for (element in array) {
            if (text == element) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun toCodePointArray(charSequence: CharSequence): IntArray {
        return toCodePointArray(charSequence, 0, charSequence.length)
    }

    private val EMPTY_CODEPOINTS = intArrayOf()

    /**
     * Converts a range of a string to an array of code points.
     * @param charSequence the source string.
     * @param startIndex the start index inside the string in java chars, inclusive.
     * @param endIndex the end index inside the string in java chars, exclusive.
     * @return a new array of code points. At most endIndex - startIndex, but possibly less.
     */
    @JvmStatic
    fun toCodePointArray(charSequence: CharSequence, startIndex: Int, endIndex: Int): IntArray {
        val length = charSequence.length
        if (length <= 0) {
            return EMPTY_CODEPOINTS
        }
        val codePoints = IntArray(Character.codePointCount(charSequence, startIndex, endIndex))
        copyCodePointsAndReturnCodePointCount(
            codePoints, charSequence, startIndex, endIndex,
            false /* downCase */
        )
        return codePoints
    }

    /**
     * Copies the codepoints in a CharSequence to an int array.
     *
     * This method assumes there is enough space in the array to store the code points. The size
     * can be measured with Character#codePointCount(CharSequence, int, int) before passing to this
     * method. If the int array is too small, an ArrayIndexOutOfBoundsException will be thrown.
     * Also, this method makes no effort to be thread-safe. Do not modify the CharSequence while
     * this method is running, or the behavior is undefined.
     * This method can optionally downcase code points before copying them, but it pays no attention
     * to locale while doing so.
     *
     * @param destination the int array.
     * @param charSequence the CharSequence.
     * @param startIndex the start index inside the string in java chars, inclusive.
     * @param endIndex the end index inside the string in java chars, exclusive.
     * @param downCase if this is true, code points will be downcased before being copied.
     * @return the number of copied code points.
     */
    @JvmStatic
    fun copyCodePointsAndReturnCodePointCount(
        destination: IntArray, charSequence: CharSequence,
        startIndex: Int, endIndex: Int, downCase: Boolean
    ): Int {
        var destIndex = 0
        var index = startIndex
        while (index < endIndex) {
            val codePoint = Character.codePointAt(charSequence, index)
            // TODO: stop using this, as it's not aware of the locale and does not always do
            // the right thing.
            destination[destIndex] = if (downCase) Character.toLowerCase(codePoint) else codePoint
            destIndex++
            index = Character.offsetByCodePoints(charSequence, index, 1)
        }
        return destIndex
    }

    @JvmStatic
    fun toSortedCodePointArray(string: String): IntArray {
        val codePoints = toCodePointArray(string)
        Arrays.sort(codePoints)
        return codePoints
    }

    @JvmStatic
    fun isIdenticalAfterUpcase(text: String): Boolean {
        val length = text.length
        var i = 0
        while (i < length) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint) && !Character.isUpperCase(codePoint)) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }

    @JvmStatic
    fun isIdenticalAfterDowncase(text: String): Boolean {
        val length = text.length
        var i = 0
        while (i < length) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint) && !Character.isLowerCase(codePoint)) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }

    @JvmStatic
    fun isIdenticalAfterCapitalizeEachWord(text: String): Boolean {
        var needsCapsNext = true
        val len = text.length
        var i = 0
        while (i < len) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint)) {
                if ((needsCapsNext && !Character.isUpperCase(codePoint))
                    || (!needsCapsNext && !Character.isLowerCase(codePoint))
                ) {
                    return false
                }
            }
            // We need a capital letter next if this is a whitespace.
            needsCapsNext = Character.isWhitespace(codePoint)
            i = text.offsetByCodePoints(i, 1)
        }
        return true
    }

    // TODO: like capitalizeFirst*, this does not work perfectly for Dutch because of the IJ digraph
    // which should be capitalized together in *some* cases.
    @JvmStatic
    fun capitalizeEachWord(text: String, locale: Locale): String {
        val builder = StringBuilder()
        var needsCapsNext = true
        val len = text.length
        var i = 0
        while (i < len) {
            val nextChar = text.substring(i, text.offsetByCodePoints(i, 1))
            if (needsCapsNext) {
                builder.append(toTitleCaseOfKeyLabel(nextChar, locale))
            } else {
                builder.append(toLowerCaseOfKeyLabel(nextChar, locale))
            }
            // We need a capital letter next if this is a whitespace.
            needsCapsNext = Character.isWhitespace(nextChar.codePointAt(0))
            i = text.offsetByCodePoints(i, 1)
        }
        return builder.toString()
    }

    private const val LANGUAGE_GREEK = "el"

    private fun getLocaleUsedForToTitleCase(locale: Locale): Locale {
        // In Greek locale String.toUpperCase(Locale) eliminates accents from its result.
        // In order to get accented upper case letter, Locale.ROOT should be used.
        return if (LANGUAGE_GREEK == locale.language) {
            Locale.ROOT
        } else {
            locale
        }
    }

    @JvmStatic
    fun toLowerCase(text: String, locale: Locale): String {
        val builder = StringBuilder()
        val len = text.length
        var i = 0
        while (i < len) {
            val nextChar = text.substring(i, text.offsetByCodePoints(i, 1))
            builder.append(toLowerCaseOfKeyLabel(nextChar, locale))
            i = text.offsetByCodePoints(i, 1)
        }
        return builder.toString()
    }

    @JvmStatic
    fun toUpperCase(text: String, locale: Locale): String {
        val builder = StringBuilder()
        val len = text.length
        var i = 0
        while (i < len) {
            val nextChar = text.substring(i, text.offsetByCodePoints(i, 1))
            builder.append(toTitleCaseOfKeyLabel(nextChar, locale))
            i = text.offsetByCodePoints(i, 1)
        }
        return builder.toString()
    }

    @JvmStatic
    fun toLowerCaseOfKeyLabel(label: String?, locale: Locale): String? {
        if (label == null) {
            return null
        }
        return when (label) {
            "\u1E9E" -> // sharp S (ß, U+00DF) => ẞ (U+1E9E), not 'SS'.
                "\u00DF"
            else -> label.toLowerCase(getLocaleUsedForToTitleCase(locale))
        }
    }

    @JvmStatic
    fun toTitleCaseOfKeyLabel(label: String?, locale: Locale): String? {
        if (label == null) {
            return null
        }
        return when (label) {
            "\u00DF" -> // sharp S (ß, U+00DF) => ẞ (U+1E9E), not 'SS'.
                "\u1E9E"
            else -> label.toUpperCase(getLocaleUsedForToTitleCase(locale))
        }
    }

    @JvmStatic
    fun toTitleCaseOfKeyCode(code: Int, locale: Locale): Int {
        if (!Constants.isLetterCode(code)) {
            return code
        }
        val label = newSingleCodePointString(code)
        val titleCaseLabel = toTitleCaseOfKeyLabel(label, locale)
        return if (codePointCount(titleCaseLabel) == 1) {
            titleCaseLabel!!.codePointAt(0)
        } else {
            Constants.CODE_UNSPECIFIED
        }
    }
}
