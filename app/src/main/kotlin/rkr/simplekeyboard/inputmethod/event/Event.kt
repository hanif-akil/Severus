/*
 * Copyright (C) 2012 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.event

import rkr.simplekeyboard.inputmethod.latin.common.StringUtils

class Event private constructor(
    private val mEventType: Int,
    val mText: CharSequence?,
    val mCodePoint: Int,
    val mKeyCode: Int,
    private val mFlags: Int,
    val mNextEvent: Event?
) {

    companion object {
        const val EVENT_TYPE_NOT_HANDLED = 0
        const val EVENT_TYPE_INPUT_KEYPRESS = 1
        const val EVENT_TYPE_TOGGLE = 2
        const val EVENT_TYPE_MODE_KEY = 3
        const val EVENT_TYPE_SOFTWARE_GENERATED_STRING = 6
        const val EVENT_TYPE_CURSOR_MOVE = 7

        const val NOT_A_CODE_POINT = -1
        const val NOT_A_KEY_CODE = 0

        private const val FLAG_NONE = 0
        private const val FLAG_REPEAT = 0x2
        private const val FLAG_CONSUMED = 0x4

        @JvmStatic
        fun createSoftwareKeypressEvent(codePoint: Int, keyCode: Int, isKeyRepeat: Boolean): Event {
            return Event(
                EVENT_TYPE_INPUT_KEYPRESS, null, codePoint, keyCode,
                if (isKeyRepeat) FLAG_REPEAT else FLAG_NONE, null
            )
        }

        @JvmStatic
        fun createSoftwareTextEvent(text: CharSequence?, keyCode: Int): Event {
            return Event(
                EVENT_TYPE_SOFTWARE_GENERATED_STRING, text, NOT_A_CODE_POINT, keyCode,
                FLAG_NONE, null
            )
        }
    }

    fun isFunctionalKeyEvent(): Boolean {
        return NOT_A_CODE_POINT == mCodePoint
    }

    fun isKeyRepeat(): Boolean {
        return 0 != (FLAG_REPEAT and mFlags)
    }

    fun isConsumed(): Boolean {
        return 0 != (FLAG_CONSUMED and mFlags)
    }

    fun getTextToCommit(): CharSequence {
        if (isConsumed()) {
            return ""
        }
        return when (mEventType) {
            EVENT_TYPE_MODE_KEY, EVENT_TYPE_NOT_HANDLED, EVENT_TYPE_TOGGLE, EVENT_TYPE_CURSOR_MOVE -> ""
            EVENT_TYPE_INPUT_KEYPRESS -> StringUtils.newSingleCodePointString(mCodePoint)
            EVENT_TYPE_SOFTWARE_GENERATED_STRING -> mText ?: ""
            else -> throw RuntimeException("Unknown event type: $mEventType")
        }
    }
}
