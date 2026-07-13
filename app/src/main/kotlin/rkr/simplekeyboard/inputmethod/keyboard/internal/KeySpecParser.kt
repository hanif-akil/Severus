/*
 * Copyright (C) 2010 The Android Open Source Project
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

import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.Constants.CODE_OUTPUT_TEXT
import rkr.simplekeyboard.inputmethod.latin.common.Constants.CODE_UNSPECIFIED
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils

object KeySpecParser {
    private val BACKSLASH = Constants.CODE_BACKSLASH
    private val VERTICAL_BAR = Constants.CODE_VERTICAL_BAR
    private const val PREFIX_HEX = "0x"

    private fun hasIcon(keySpec: String): Boolean {
        return keySpec.startsWith(KeyboardIconsSet.PREFIX_ICON)
    }

    private fun hasCode(keySpec: String, labelEnd: Int): Boolean {
        if (labelEnd <= 0 || labelEnd + 1 >= keySpec.length) return false
        if (keySpec.startsWith(KeyboardCodesSet.PREFIX_CODE, labelEnd + 1)) return true
        return keySpec.startsWith(PREFIX_HEX, labelEnd + 1)
    }

    private fun parseEscape(text: String): String {
        if (text.indexOf(BACKSLASH) < 0) return text
        val length = text.length
        val sb = StringBuilder()
        var pos = 0
        while (pos < length) {
            val c = text[pos]
            if (c == BACKSLASH && pos + 1 < length) {
                pos++
                sb.append(text[pos])
            } else {
                sb.append(c)
            }
            pos++
        }
        return sb.toString()
    }

    private fun indexOfLabelEnd(keySpec: String): Int {
        val length = keySpec.length
        if (keySpec.indexOf(BACKSLASH) < 0) {
            val labelEnd = keySpec.indexOf(VERTICAL_BAR)
            if (labelEnd == 0) {
                if (length == 1) return -1
                throw KeySpecParserError("Empty label")
            }
            return labelEnd
        }
        var pos = 0
        while (pos < length) {
            val c = keySpec[pos]
            if (c == BACKSLASH && pos + 1 < length) {
                pos++
            } else if (c == VERTICAL_BAR) {
                return pos
            }
            pos++
        }
        return -1
    }

    private fun getBeforeLabelEnd(keySpec: String, labelEnd: Int): String {
        return if (labelEnd < 0) keySpec else keySpec.substring(0, labelEnd)
    }

    private fun getAfterLabelEnd(keySpec: String, labelEnd: Int): String {
        return keySpec.substring(labelEnd + 1)
    }

    private fun checkDoubleLabelEnd(keySpec: String, labelEnd: Int) {
        if (indexOfLabelEnd(getAfterLabelEnd(keySpec, labelEnd)) < 0) return
        throw KeySpecParserError("Multiple $VERTICAL_BAR: $keySpec")
    }

    @JvmStatic
    fun getLabel(keySpec: String?): String? {
        if (keySpec == null) return null
        if (hasIcon(keySpec)) return null
        val labelEnd = indexOfLabelEnd(keySpec)
        val label = parseEscape(getBeforeLabelEnd(keySpec, labelEnd))
        if (label.isEmpty()) throw KeySpecParserError("Empty label: $keySpec")
        return label
    }

    private fun getOutputTextInternal(keySpec: String, labelEnd: Int): String? {
        if (labelEnd <= 0) return null
        checkDoubleLabelEnd(keySpec, labelEnd)
        return parseEscape(getAfterLabelEnd(keySpec, labelEnd))
    }

    @JvmStatic
    fun getOutputText(keySpec: String?): String? {
        if (keySpec == null) return null
        val labelEnd = indexOfLabelEnd(keySpec)
        if (hasCode(keySpec, labelEnd)) return null
        val outputText = getOutputTextInternal(keySpec, labelEnd)
        if (outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) return null
            if (outputText.isEmpty()) throw KeySpecParserError("Empty outputText: $keySpec")
            return outputText
        }
        val label = getLabel(keySpec)
        if (label == null) throw KeySpecParserError("Empty label: $keySpec")
        return if (StringUtils.codePointCount(label) == 1) null else label
    }

    @JvmStatic
    fun getCode(keySpec: String?): Int {
        if (keySpec == null) return CODE_UNSPECIFIED
        val labelEnd = indexOfLabelEnd(keySpec)
        if (hasCode(keySpec, labelEnd)) {
            checkDoubleLabelEnd(keySpec, labelEnd)
            return parseCode(getAfterLabelEnd(keySpec, labelEnd), CODE_UNSPECIFIED)
        }
        val outputText = getOutputTextInternal(keySpec, labelEnd)
        if (outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) return outputText.codePointAt(0)
            return CODE_OUTPUT_TEXT
        }
        val label = getLabel(keySpec)
        if (label == null) throw KeySpecParserError("Empty label: $keySpec")
        return if (StringUtils.codePointCount(label) == 1) label.codePointAt(0) else CODE_OUTPUT_TEXT
    }

    @JvmStatic
    fun parseCode(text: String?, defaultCode: Int): Int {
        if (text == null) return defaultCode
        if (text.startsWith(KeyboardCodesSet.PREFIX_CODE)) {
            return KeyboardCodesSet.getCode(text.substring(KeyboardCodesSet.PREFIX_CODE.length))
        }
        if (text.startsWith(PREFIX_HEX)) {
            return Integer.parseInt(text.substring(PREFIX_HEX.length), 16)
        }
        return defaultCode
    }

    @JvmStatic
    fun getIconId(keySpec: String?): Int {
        if (keySpec == null) return KeyboardIconsSet.ICON_UNDEFINED
        if (!hasIcon(keySpec)) return KeyboardIconsSet.ICON_UNDEFINED
        val labelEnd = indexOfLabelEnd(keySpec)
        val iconName = getBeforeLabelEnd(keySpec, labelEnd).substring(KeyboardIconsSet.PREFIX_ICON.length)
        return KeyboardIconsSet.getIconId(iconName)
    }

    class KeySpecParserError(message: String) : RuntimeException(message)
}
