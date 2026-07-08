package rkr.simplekeyboard.inputmethod.latin.common

object Constants {
    object Color {
        const val ALPHA_OPAQUE = 255
    }

    object TextUtils {
        const val CAP_MODE_OFF = 0
    }

    const val NOT_A_CODE = -1
    const val NOT_A_COORDINATE = -1
    const val EDITOR_CONTENTS_CACHE_SIZE = 1024
    const val MAX_CHARACTERS_FOR_RECAPITALIZATION = 1024 * 100
    const val CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER = 1
    const val CODE_ENTER = '\n'.code
    const val CODE_TAB = '\t'.code
    const val CODE_SPACE = ' '.code
    const val CODE_PERIOD = '.'.code
    const val CODE_COMMA = ','.code
    const val CODE_SINGLE_QUOTE = '\''.code
    const val CODE_DOUBLE_QUOTE = '"'.code
    const val CODE_BACKSLASH = '\\'.code
    const val CODE_VERTICAL_BAR = '|'.code
    const val CODE_PERCENT = '%'.code
    const val CODE_INVERTED_QUESTION_MARK = 0xBF
    const val CODE_INVERTED_EXCLAMATION_MARK = 0xA1
    const val CODE_SHIFT = -1
    const val CODE_CAPSLOCK = -2
    const val CODE_SWITCH_ALPHA_SYMBOL = -3
    const val CODE_OUTPUT_TEXT = -4
    const val CODE_DELETE = -5
    const val CODE_SETTINGS = -6
    const val CODE_PASTE = -7
    const val CODE_ACTION_NEXT = -8
    const val CODE_ACTION_PREVIOUS = -9
    const val CODE_LANGUAGE_SWITCH = -10
    const val CODE_SHIFT_ENTER = -11
    const val CODE_SYMBOL_SHIFT = -12
    const val CODE_UNSPECIFIED = -13
    const val CODE_CLIPBOARD_HISTORY = -14
    const val SCREEN_METRICS_LARGE_TABLET = 2
    const val SCREEN_METRICS_SMALL_TABLET = 3

    fun isValidCoordinate(coordinate: Int): Boolean = coordinate >= 0

    fun isLetterCode(code: Int): Boolean = code >= CODE_SPACE

    fun printableCode(code: Int): String = when (code) {
        CODE_SHIFT -> "shift"
        CODE_CAPSLOCK -> "capslock"
        CODE_SWITCH_ALPHA_SYMBOL -> "symbol"
        CODE_OUTPUT_TEXT -> "text"
        CODE_DELETE -> "delete"
        CODE_SETTINGS -> "settings"
        CODE_PASTE -> "paste"
        CODE_ACTION_NEXT -> "actionNext"
        CODE_ACTION_PREVIOUS -> "actionPrevious"
        CODE_LANGUAGE_SWITCH -> "languageSwitch"
        CODE_SHIFT_ENTER -> "shiftEnter"
        CODE_CLIPBOARD_HISTORY -> "clipboardHistory"
        CODE_UNSPECIFIED -> "unspec"
        CODE_TAB -> "tab"
        CODE_ENTER -> "enter"
        CODE_SPACE -> "space"
        else -> when {
            code < CODE_SPACE -> String.format("\\u%02X", code)
            code < 0x100 -> String.format("%c", code)
            code < 0x10000 -> String.format("\\u%04X", code)
            else -> String.format("\\U%05X", code)
        }
    }
}
