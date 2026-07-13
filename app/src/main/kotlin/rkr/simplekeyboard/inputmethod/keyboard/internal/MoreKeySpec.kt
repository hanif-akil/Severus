/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2020 wittmane
 * Copyright (C) 2019 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.text.TextUtils
import android.util.SparseIntArray
import java.util.Locale
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.latin.common.CollectionUtils
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils

class MoreKeySpec(
    moreKeySpec: String,
    needsToUpperCase: Boolean,
    locale: Locale
) {
    val mCode: Int
    val mLabel: String?
    val mOutputText: String?
    val mIconId: Int

    init {
        if (moreKeySpec.isEmpty()) throw KeySpecParser.KeySpecParserError("Empty more key spec")
        val label = KeySpecParser.getLabel(moreKeySpec)
        mLabel = if (needsToUpperCase) StringUtils.toTitleCaseOfKeyLabel(label, locale) else label
        val codeInSpec = KeySpecParser.getCode(moreKeySpec)
        val code = if (needsToUpperCase) StringUtils.toTitleCaseOfKeyCode(codeInSpec, locale) else codeInSpec
        if (code == Constants.CODE_UNSPECIFIED) {
            mCode = Constants.CODE_OUTPUT_TEXT
            mOutputText = mLabel
        } else {
            mCode = code
            val outputText = KeySpecParser.getOutputText(moreKeySpec)
            mOutputText = if (needsToUpperCase) StringUtils.toTitleCaseOfKeyLabel(outputText, locale) else outputText
        }
        mIconId = KeySpecParser.getIconId(moreKeySpec)
    }

    fun buildKey(
        x: Float, y: Float, width: Float, height: Float,
        leftPadding: Float, rightPadding: Float, topPadding: Float,
        bottomPadding: Float, labelFlags: Int
    ): Key {
        return Key(
            mLabel, mIconId, mCode, mOutputText, null, labelFlags,
            Key.BACKGROUND_TYPE_NORMAL, x, y, width, height, leftPadding, rightPadding,
            topPadding, bottomPadding
        )
    }

    override fun hashCode(): Int {
        var hashCode = 31 + mCode
        hashCode = hashCode * 31 + mIconId
        val label = mLabel
        hashCode = hashCode * 31 + (label?.hashCode() ?: 0)
        val outputText = mOutputText
        hashCode = hashCode * 31 + (outputText?.hashCode() ?: 0)
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is MoreKeySpec) {
            return mCode == other.mCode
                    && mIconId == other.mIconId
                    && TextUtils.equals(mLabel, other.mLabel)
                    && TextUtils.equals(mOutputText, other.mOutputText)
        }
        return false
    }

    override fun toString(): String {
        val label = if (mIconId == KeyboardIconsSet.ICON_UNDEFINED) mLabel
            else KeyboardIconsSet.PREFIX_ICON + KeyboardIconsSet.getIconName(mIconId)
        val output = if (mCode == Constants.CODE_OUTPUT_TEXT) mOutputText
            else Constants.printableCode(mCode)
        if (label != null && StringUtils.codePointCount(label) == 1 && label.codePointAt(0) == mCode) {
            return output ?: ""
        }
        return "$label|$output"
    }

    class LettersOnBaseLayout {
        private val mCodes = SparseIntArray()
        private val mTexts = HashSet<String>()

        fun addLetter(key: Key) {
            val code = key.code
            if (Character.isAlphabetic(code)) {
                mCodes.put(code, 0)
            } else if (code == Constants.CODE_OUTPUT_TEXT) {
                mTexts.add(key.outputText!!)
            }
        }

        fun contains(moreKey: MoreKeySpec): Boolean {
            val code = moreKey.mCode
            return if (Character.isAlphabetic(code) && mCodes.indexOfKey(code) >= 0) {
                true
            } else code == Constants.CODE_OUTPUT_TEXT && mTexts.contains(moreKey.mOutputText)
        }
    }

    companion object {
        private val COMMA = Constants.CODE_COMMA
        private val BACKSLASH = Constants.CODE_BACKSLASH
        private val ADDITIONAL_MORE_KEY_MARKER = StringUtils.newSingleCodePointString(Constants.CODE_PERCENT)
        private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)

        @JvmStatic
        fun splitKeySpecs(text: String?): Array<String>? {
            if (TextUtils.isEmpty(text)) return null
            val size = text!!.length
            if (size == 1) return if (text[0] == COMMA.toChar()) null else arrayOf(text)

            var list: ArrayList<String>? = null
            var start = 0
            var pos = 0
            while (pos < size) {
                val c = text[pos]
                if (c == COMMA) {
                    if (pos - start > 0) {
                        if (list == null) list = ArrayList()
                        list.add(text.substring(start, pos))
                    }
                    start = pos + 1
                } else if (c == BACKSLASH) {
                    pos++
                }
                pos++
            }
            val remain = if (size - start > 0) text.substring(start) else null
            if (list == null) return if (remain != null) arrayOf(remain) else null
            if (remain != null) list.add(remain)
            return list.toTypedArray()
        }

        private fun filterOutEmptyString(array: Array<String>?): Array<String> {
            if (array == null) return EMPTY_STRING_ARRAY as Array<String>
            var out: ArrayList<String>? = null
            for (i in array.indices) {
                val entry = array[i]
                if (TextUtils.isEmpty(entry)) {
                    if (out == null) out = CollectionUtils.arrayAsList(array, 0, i)
                } else if (out != null) {
                    out.add(entry)
                }
            }
            return if (out == null) array else out.toTypedArray()
        }

        @JvmStatic
        fun insertAdditionalMoreKeys(
            moreKeySpecs: Array<String>?,
            additionalMoreKeySpecs: Array<String>?
        ): Array<String>? {
            val moreKeys = filterOutEmptyString(moreKeySpecs)
            val additionalMoreKeys = filterOutEmptyString(additionalMoreKeySpecs)
            val moreKeysCount = moreKeys.size
            val additionalCount = additionalMoreKeys.size
            var out: ArrayList<String>? = null
            var additionalIndex = 0
            for (moreKeyIndex in 0 until moreKeysCount) {
                val moreKeySpec = moreKeys[moreKeyIndex]
                if (moreKeySpec == ADDITIONAL_MORE_KEY_MARKER) {
                    if (additionalIndex < additionalCount) {
                        val additionalMoreKey = additionalMoreKeys[additionalIndex]
                        if (out != null) {
                            out.add(additionalMoreKey)
                        } else {
                            moreKeys[moreKeyIndex] = additionalMoreKey
                        }
                        additionalIndex++
                    } else {
                        if (out == null) out = CollectionUtils.arrayAsList(moreKeys, 0, moreKeyIndex)
                    }
                } else {
                    if (out != null) out.add(moreKeySpec)
                }
            }
            if (additionalCount > 0 && additionalIndex == 0) {
                out = CollectionUtils.arrayAsList(additionalMoreKeys, additionalIndex, additionalCount)
                for (i in 0 until moreKeysCount) {
                    out.add(moreKeys[i])
                }
            } else if (additionalIndex < additionalCount) {
                out = CollectionUtils.arrayAsList(moreKeys, 0, moreKeysCount)
                for (i in additionalIndex until additionalCount) {
                    out.add(additionalMoreKeys[additionalIndex])
                }
            }
            if (out == null && moreKeysCount > 0) return moreKeys
            return if (out != null && out.size > 0) out.toTypedArray() else null
        }

        @JvmStatic
        fun getIntValue(moreKeys: Array<String>?, key: String, defaultValue: Int): Int {
            if (moreKeys == null) return defaultValue
            val keyLen = key.length
            var foundValue = false
            var value = defaultValue
            for (i in moreKeys.indices) {
                val moreKeySpec = moreKeys[i] ?: continue
                if (!moreKeySpec.startsWith(key)) continue
                moreKeys[i] = null
                try {
                    if (!foundValue) {
                        value = Integer.parseInt(moreKeySpec.substring(keyLen))
                        foundValue = true
                    }
                } catch (e: NumberFormatException) {
                    throw RuntimeException("integer should follow after $key: $moreKeySpec")
                }
            }
            return value
        }

        @JvmStatic
        fun getBooleanValue(moreKeys: Array<String>?, key: String): Boolean {
            if (moreKeys == null) return false
            var value = false
            for (i in moreKeys.indices) {
                val moreKeySpec = moreKeys[i] ?: continue
                if (moreKeySpec != key) continue
                moreKeys[i] = null
                value = true
            }
            return value
        }

        @JvmStatic
        fun removeRedundantMoreKeys(
            moreKeys: Array<MoreKeySpec>?,
            lettersOnBaseLayout: LettersOnBaseLayout
        ): Array<MoreKeySpec>? {
            if (moreKeys == null) return null
            val filteredMoreKeys = ArrayList<MoreKeySpec>()
            for (moreKey in moreKeys) {
                if (!lettersOnBaseLayout.contains(moreKey)) {
                    filteredMoreKeys.add(moreKey)
                }
            }
            val size = filteredMoreKeys.size
            if (size == moreKeys.size) return moreKeys
            if (size == 0) return null
            return filteredMoreKeys.toTypedArray()
        }
    }
}
