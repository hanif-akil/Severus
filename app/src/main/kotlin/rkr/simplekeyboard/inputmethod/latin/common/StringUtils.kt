package rkr.simplekeyboard.inputmethod.latin.common

import android.text.TextUtils
import java.util.Locale

object StringUtils {
    private val EMPTY_CODEPOINTS = intArrayOf()

    fun codePointCount(text: CharSequence?): Int {
        if (TextUtils.isEmpty(text)) return 0
        return Character.codePointCount(text, 0, text!!.length)
    }

    fun newSingleCodePointString(codePoint: Int): String {
        if (Character.charCount(codePoint) == 1) {
            return codePoint.toChar().toString()
        }
        return String(Character.toChars(codePoint))
    }

    fun containsInArray(text: String, array: Array<String>): Boolean {
        for (element in array) {
            if (text == element) return true
        }
        return false
    }

    fun toCodePointArray(charSequence: CharSequence): IntArray {
        return toCodePointArray(charSequence, 0, charSequence.length)
    }

    fun toCodePointArray(charSequence: CharSequence, startIndex: Int, endIndex: Int): IntArray {
        val length = charSequence.length
        if (length <= 0) return EMPTY_CODEPOINTS
        val codePoints = IntArray(Character.codePointCount(charSequence, startIndex, endIndex))
        copyCodePointsAndReturnCodePointCount(codePoints, charSequence, startIndex, endIndex, false)
        return codePoints
    }

    fun copyCodePointsAndReturnCodePointCount(
        destination: IntArray,
        charSequence: CharSequence,
        startIndex: Int,
        endIndex: Int,
        downCase: Boolean
    ): Int {
        var destIndex = 0
        var index = startIndex
        while (index < endIndex) {
            val codePoint = Character.codePointAt(charSequence, index)
            destination[destIndex] = if (downCase) Character.toLowerCase(codePoint) else codePoint
            destIndex++
            index = Character.offsetByCodePoints(charSequence, index, 1)
        }
        return destIndex
    }

    fun toSortedCodePointArray(string: String): IntArray {
        val codePoints = toCodePointArray(string)
        codePoints.sort()
        return codePoints
    }

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

    fun isIdenticalAfterCapitalizeEachWord(text: String): Boolean {
        var needsCapsNext = true
        val len = text.length
        var i = 0
        while (i < len) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint)) {
                if ((needsCapsNext && !Character.isUpperCase(codePoint)) ||
                    (!needsCapsNext && !Character.isLowerCase(codePoint))
                ) {
                    return false
                }
            }
            needsCapsNext = Character.isWhitespace(codePoint)
            i = text.offsetByCodePoints(i, 1)
        }
        return true
    }

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
            needsCapsNext = Character.isWhitespace(nextChar.codePointAt(0))
            i = text.offsetByCodePoints(i, 1)
        }
        return builder.toString()
    }

    private const val LANGUAGE_GREEK = "el"

    private fun getLocaleUsedForToTitleCase(locale: Locale): Locale {
        if (LANGUAGE_GREEK == locale.language) {
            return Locale.ROOT
        }
        return locale
    }

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

    fun toLowerCaseOfKeyLabel(label: String?, locale: Locale): String? {
        if (label == null) return null
        return when (label) {
            "\u1E9E" -> "\u00DF"
            else -> label.lowercase(getLocaleUsedForToTitleCase(locale))
        }
    }

    fun toTitleCaseOfKeyLabel(label: String?, locale: Locale): String? {
        if (label == null) return null
        return when (label) {
            "\u00DF" -> "\u1E9E"
            else -> label.uppercase(getLocaleUsedForToTitleCase(locale))
        }
    }

    fun toTitleCaseOfKeyCode(code: Int, locale: Locale): Int {
        if (!Constants.isLetterCode(code)) return code
        val label = newSingleCodePointString(code)
        val titleCaseLabel = toTitleCaseOfKeyLabel(label, locale)
        return if (codePointCount(titleCaseLabel) == 1) {
            titleCaseLabel!!.codePointAt(0)
        } else {
            Constants.CODE_UNSPECIFIED
        }
    }
}
