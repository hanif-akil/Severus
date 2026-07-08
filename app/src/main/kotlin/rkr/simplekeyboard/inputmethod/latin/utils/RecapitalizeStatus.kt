package rkr.simplekeyboard.inputmethod.latin.utils

import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import java.util.Locale

class RecapitalizeStatus {
    companion object {
        const val NOT_A_RECAPITALIZE_MODE = -1
        const val CAPS_MODE_ORIGINAL_MIXED_CASE = 0
        const val CAPS_MODE_ALL_LOWER = 1
        const val CAPS_MODE_FIRST_WORD_UPPER = 2
        const val CAPS_MODE_ALL_UPPER = 3

        private val ROTATION_STYLE = intArrayOf(
            CAPS_MODE_ORIGINAL_MIXED_CASE,
            CAPS_MODE_ALL_LOWER,
            CAPS_MODE_FIRST_WORD_UPPER,
            CAPS_MODE_ALL_UPPER
        )

        private fun getStringMode(string: String): Int {
            return when {
                StringUtils.isIdenticalAfterUpcase(string) -> CAPS_MODE_ALL_UPPER
                StringUtils.isIdenticalAfterDowncase(string) -> CAPS_MODE_ALL_LOWER
                StringUtils.isIdenticalAfterCapitalizeEachWord(string) -> CAPS_MODE_FIRST_WORD_UPPER
                else -> CAPS_MODE_ORIGINAL_MIXED_CASE
            }
        }

        fun modeToString(recapitalizeMode: Int): String = when (recapitalizeMode) {
            NOT_A_RECAPITALIZE_MODE -> "undefined"
            CAPS_MODE_ORIGINAL_MIXED_CASE -> "mixedCase"
            CAPS_MODE_ALL_LOWER -> "allLower"
            CAPS_MODE_FIRST_WORD_UPPER -> "firstWordUpper"
            CAPS_MODE_ALL_UPPER -> "allUpper"
            else -> "unknown<$recapitalizeMode>"
        }
    }

    private var mCursorStartBefore = 0
    private var mStringBefore: String = ""
    private var mCursorStartAfter = 0
    private var mCursorEndAfter = 0
    private var mRotationStyleCurrentIndex = 0
    private var mSkipOriginalMixedCaseMode = false
    private var mLocale: Locale = Locale.getDefault()
    private var mStringAfter: String = ""
    private var mIsStarted = false
    var isEnabled = true
        private set

    init {
        start(-1, -1, "", Locale.getDefault())
        stop()
    }

    fun start(cursorStart: Int, cursorEnd: Int, string: String, locale: Locale) {
        if (!isEnabled) return
        mCursorStartBefore = cursorStart
        mStringBefore = string
        mCursorStartAfter = cursorStart
        mCursorEndAfter = cursorEnd
        mStringAfter = string
        val initialMode = getStringMode(mStringBefore)
        mLocale = locale
        if (CAPS_MODE_ORIGINAL_MIXED_CASE == initialMode) {
            mRotationStyleCurrentIndex = 0
            mSkipOriginalMixedCaseMode = false
        } else {
            var currentMode: Int
            currentMode = ROTATION_STYLE.size - 1
            while (currentMode > 0) {
                if (ROTATION_STYLE[currentMode] == initialMode) break
                currentMode--
            }
            mRotationStyleCurrentIndex = currentMode
            mSkipOriginalMixedCaseMode = true
        }
        mIsStarted = true
    }

    fun stop() {
        mIsStarted = false
    }

    fun isStarted(): Boolean = mIsStarted

    fun isSetAt(cursorStart: Int, cursorEnd: Int): Boolean {
        return cursorStart == mCursorStartAfter && cursorEnd == mCursorEndAfter
    }

    fun rotate() {
        val oldResult = mStringAfter
        var count = 0
        do {
            mRotationStyleCurrentIndex = (mRotationStyleCurrentIndex + 1) % ROTATION_STYLE.size
            if (CAPS_MODE_ORIGINAL_MIXED_CASE == ROTATION_STYLE[mRotationStyleCurrentIndex] &&
                mSkipOriginalMixedCaseMode
            ) {
                mRotationStyleCurrentIndex = (mRotationStyleCurrentIndex + 1) % ROTATION_STYLE.size
            }
            count++
            mStringAfter = when (ROTATION_STYLE[mRotationStyleCurrentIndex]) {
                CAPS_MODE_ORIGINAL_MIXED_CASE -> mStringBefore
                CAPS_MODE_ALL_LOWER -> StringUtils.toLowerCase(mStringBefore, mLocale)
                CAPS_MODE_FIRST_WORD_UPPER -> StringUtils.capitalizeEachWord(mStringBefore, mLocale)
                CAPS_MODE_ALL_UPPER -> StringUtils.toUpperCase(mStringBefore, mLocale)
                else -> mStringBefore
            }
        } while (mStringAfter == oldResult && count < ROTATION_STYLE.size + 1)
        mCursorEndAfter = mCursorStartAfter + mStringAfter.length
    }

    fun trim() {
        val len = mStringBefore.length
        var nonWhitespaceStart = 0
        while (nonWhitespaceStart < len) {
            val codePoint = mStringBefore.codePointAt(nonWhitespaceStart)
            if (!Character.isWhitespace(codePoint)) break
            nonWhitespaceStart = mStringBefore.offsetByCodePoints(nonWhitespaceStart, 1)
        }
        var nonWhitespaceEnd = len
        while (nonWhitespaceEnd > 0) {
            val codePoint = mStringBefore.codePointBefore(nonWhitespaceEnd)
            if (!Character.isWhitespace(codePoint)) break
            nonWhitespaceEnd = mStringBefore.offsetByCodePoints(nonWhitespaceEnd, -1)
        }
        if ((0 != nonWhitespaceStart || len != nonWhitespaceEnd)
            && nonWhitespaceStart < nonWhitespaceEnd
        ) {
            mCursorEndAfter = mCursorStartBefore + nonWhitespaceEnd
            mCursorStartBefore = mCursorStartBefore + nonWhitespaceStart
            mCursorStartAfter = mCursorStartBefore
            mStringBefore = mStringBefore.substring(nonWhitespaceStart, nonWhitespaceEnd)
            mStringAfter = mStringBefore
        }
    }

    fun getRecapitalizedString(): String = mStringAfter

    fun getNewCursorStart(): Int = mCursorStartAfter

    fun getNewCursorEnd(): Int = mCursorEndAfter

    fun getCurrentMode(): Int = ROTATION_STYLE[mRotationStyleCurrentIndex]
}
