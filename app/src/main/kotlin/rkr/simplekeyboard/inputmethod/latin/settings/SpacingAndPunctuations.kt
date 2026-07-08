package rkr.simplekeyboard.inputmethod.latin.settings

import android.content.res.Resources
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import java.util.Locale

class SpacingAndPunctuations(res: Resources) {
    val mSortedWordSeparators: IntArray = StringUtils.toSortedCodePointArray(res.getString(R.string.symbols_word_separators))
    private val mSentenceSeparator: Int = res.getInteger(R.integer.sentence_separator)
    private val mAbbreviationMarker: Int = res.getInteger(R.integer.abbreviation_marker)
    private val mSortedSentenceTerminators: IntArray = StringUtils.toSortedCodePointArray(res.getString(R.string.symbols_sentence_terminators))
    val mUsesAmericanTypography: Boolean = Locale.ENGLISH.language == res.configuration.locale.language
    val mUsesGermanRules: Boolean = Locale.GERMAN.language == res.configuration.locale.language

    fun isWordSeparator(code: Int): Boolean = java.util.Arrays.binarySearch(mSortedWordSeparators, code) >= 0
    fun isSentenceTerminator(code: Int): Boolean = java.util.Arrays.binarySearch(mSortedSentenceTerminators, code) >= 0
    fun isAbbreviationMarker(code: Int): Boolean = code == mAbbreviationMarker
    fun isSentenceSeparator(code: Int): Boolean = code == mSentenceSeparator
}
