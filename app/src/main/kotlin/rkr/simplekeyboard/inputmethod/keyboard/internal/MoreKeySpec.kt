package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.text.TextUtils
import android.util.SparseIntArray
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.latin.common.CollectionUtils
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import java.util.Locale

class MoreKeySpec(moreKeySpec: String, needsToUpperCase: Boolean, locale: Locale) {
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
        leftPadding: Float, rightPadding: Float, topPadding: Float, bottomPadding: Float,
        labelFlags: Int
    ): Key {
        return Key(mLabel, mIconId, mCode, mOutputText, null, labelFlags,
            Key.BACKGROUND_TYPE_NORMAL, x, y, width, height, leftPadding, rightPadding,
            topPadding, bottomPadding)
    }

    override fun hashCode(): Int {
        var hashCode = 31 + mCode
        hashCode = hashCode * 31 + mIconId
        hashCode = hashCode * 31 + (mLabel?.hashCode() ?: 0)
        hashCode = hashCode * 31 + (mOutputText?.hashCode() ?: 0)
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is MoreKeySpec) {
            return mCode == other.mCode && mIconId == other.mIconId &&
                    TextUtils.equals(mLabel, other.mLabel) &&
                    TextUtils.equals(mOutputText, other.mOutputText)
        }
        return false
    }

    override fun toString(): String {
        val label = if (mIconId == KeyboardIconsSet.ICON_UNDEFINED) mLabel
        else KeyboardIconsSet.PREFIX_ICON + KeyboardIconsSet.getIconName(mIconId)
        val output = if (mCode == Constants.CODE_OUTPUT_TEXT) mOutputText
        else Constants.printableCode(mCode)
        return if (StringUtils.codePointCount(label) == 1 && label!!.codePointAt(0) == mCode) output!!
        else "$label|$output"
    }

    class LettersOnBaseLayout {
        private val mCodes = SparseIntArray()
        private val mTexts = HashSet<String>()

        fun addLetter(key: Key) {
            val code = key.getCode()
            if (Character.isAlphabetic(code)) {
                mCodes.put(code, 0)
            } else if (code == Constants.CODE_OUTPUT_TEXT) {
                key.getOutputText()?.let { mTexts.add(it) }
            }
        }

        fun contains(moreKey: MoreKeySpec): Boolean {
            val code = moreKey.mCode
            if (Character.isAlphabetic(code) && mCodes.indexOfKey(code) >= 0) return true
            if (code == Constants.CODE_OUTPUT_TEXT && moreKey.mOutputText != null && mTexts.contains(moreKey.mOutputText)) return true
            return false
        }
    }

    companion object {
        private val COMMA = Constants.CODE_COMMA.toChar()
        private val BACKSLASH = Constants.CODE_BACKSLASH.toChar()
        private val ADDITIONAL_MORE_KEY_MARKER = StringUtils.newSingleCodePointString(Constants.CODE_PERCENT)

        fun removeRedundantMoreKeys(moreKeys: Array<MoreKeySpec>?, lettersOnBaseLayout: LettersOnBaseLayout): Array<MoreKeySpec>? {
            if (moreKeys == null) return null
            val filteredMoreKeys = ArrayList<MoreKeySpec>()
            for (moreKey in moreKeys) {
                if (!lettersOnBaseLayout.contains(moreKey)) filteredMoreKeys.add(moreKey)
            }
            val size = filteredMoreKeys.size
            if (size == moreKeys.size) return moreKeys
            if (size == 0) return null
            return filteredMoreKeys.toTypedArray()
        }

        fun splitKeySpecs(text: String?): Array<String>? {
            if (TextUtils.isEmpty(text)) return null
            val size = text!!.length
            if (size == 1) return if (text[0] == COMMA) null else arrayOf(text)

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
            if (remain != null) list!!.add(remain)
            return list!!.toTypedArray()
        }

        private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)

        private fun filterOutEmptyString(array: Array<String>?): Array<String?> {
            if (array == null) return EMPTY_STRING_ARRAY as Array<String>
            var out: ArrayList<String>? = null
            for (i in array.indices) {
                val entry = array[i]
                if (TextUtils.isEmpty(entry)) {
                    if (out == null) out = CollectionUtils.arrayAsList(array as Array<Any>, 0, i) as ArrayList<String>
                } else if (out != null) {
                    out.add(entry)
                }
            }
            return out?.toTypedArray() ?: array
        }

        fun insertAdditionalMoreKeys(moreKeySpecs: Array<String>?, additionalMoreKeySpecs: Array<String>?): Array<String>? {
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
                        if (out != null) out.add(additionalMoreKey) else moreKeys[moreKeyIndex] = additionalMoreKey
                        additionalIndex++
                    } else {
                        if (out == null) out = CollectionUtils.arrayAsList(moreKeys as Array<Any>, 0, moreKeyIndex) as ArrayList<String>
                    }
                } else {
                    out?.add(moreKeySpec)
                }
            }
            if (additionalCount > 0 && additionalIndex == 0) {
                out = CollectionUtils.arrayAsList(additionalMoreKeys as Array<Any>, additionalIndex, additionalCount) as ArrayList<String>
                for (i in 0 until moreKeysCount) out.add(moreKeys[i])
            } else if (additionalIndex < additionalCount) {
                out = CollectionUtils.arrayAsList(moreKeys as Array<Any>, 0, moreKeysCount) as ArrayList<String>
                for (i in additionalIndex until additionalCount) out.add(additionalMoreKeys[additionalIndex])
            }
            return if (out == null && moreKeysCount > 0) moreKeys
            else if (out != null && out.size > 0) out.toTypedArray()
            else null
        }

        fun getIntValue(moreKeys: Array<String?>?, key: String, defaultValue: Int): Int {
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
                        value = moreKeySpec.substring(keyLen).toInt()
                        foundValue = true
                    }
                } catch (e: NumberFormatException) {
                    throw RuntimeException("integer should follow after $key: $moreKeySpec")
                }
            }
            return value
        }

        fun getBooleanValue(moreKeys: Array<String?>?, key: String): Boolean {
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
    }
}
