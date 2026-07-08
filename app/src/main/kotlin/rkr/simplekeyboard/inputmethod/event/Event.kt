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

        fun createSoftwareKeypressEvent(codePoint: Int, keyCode: Int, isKeyRepeat: Boolean): Event {
            return Event(
                EVENT_TYPE_INPUT_KEYPRESS, null, codePoint, keyCode,
                if (isKeyRepeat) FLAG_REPEAT else FLAG_NONE, null
            )
        }

        fun createSoftwareTextEvent(text: CharSequence, keyCode: Int): Event {
            return Event(EVENT_TYPE_SOFTWARE_GENERATED_STRING, text, NOT_A_CODE_POINT, keyCode, FLAG_NONE, null)
        }
    }

    fun isFunctionalKeyEvent(): Boolean = NOT_A_CODE_POINT == mCodePoint

    fun isKeyRepeat(): Boolean = FLAG_REPEAT and mFlags != 0

    fun isConsumed(): Boolean = FLAG_CONSUMED and mFlags != 0

    fun getTextToCommit(): CharSequence {
        if (isConsumed()) return ""
        return when (mEventType) {
            EVENT_TYPE_MODE_KEY, EVENT_TYPE_NOT_HANDLED, EVENT_TYPE_TOGGLE, EVENT_TYPE_CURSOR_MOVE -> ""
            EVENT_TYPE_INPUT_KEYPRESS -> StringUtils.newSingleCodePointString(mCodePoint)
            EVENT_TYPE_SOFTWARE_GENERATED_STRING -> mText ?: ""
            else -> throw RuntimeException("Unknown event type: $mEventType")
        }
    }
}
