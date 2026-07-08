package rkr.simplekeyboard.inputmethod.keyboard.internal

import rkr.simplekeyboard.inputmethod.latin.common.Constants

object KeyboardCodesSet {
    const val PREFIX_CODE = "!code/"

    private val sNameToIdMap = HashMap<String, Int>()

    private val ID_TO_NAME = arrayOf(
        "key_tab", "key_enter", "key_space", "key_shift", "key_capslock",
        "key_switch_alpha_symbol", "key_output_text", "key_delete", "key_settings",
        "key_paste", "key_action_next", "key_action_previous", "key_shift_enter",
        "key_language_switch", "key_left", "key_right", "key_unspecified", "key_clipboard_history"
    )

    private val DEFAULT = intArrayOf(
        Constants.CODE_TAB, Constants.CODE_ENTER, Constants.CODE_SPACE,
        Constants.CODE_SHIFT, Constants.CODE_CAPSLOCK, Constants.CODE_SWITCH_ALPHA_SYMBOL,
        Constants.CODE_OUTPUT_TEXT, Constants.CODE_DELETE, Constants.CODE_SETTINGS,
        Constants.CODE_PASTE, Constants.CODE_ACTION_NEXT, Constants.CODE_ACTION_PREVIOUS,
        Constants.CODE_SHIFT_ENTER, Constants.CODE_LANGUAGE_SWITCH,
        Constants.CODE_UNSPECIFIED, Constants.CODE_UNSPECIFIED, Constants.CODE_UNSPECIFIED,
        Constants.CODE_CLIPBOARD_HISTORY
    )

    init {
        for (i in ID_TO_NAME.indices) {
            sNameToIdMap[ID_TO_NAME[i]] = i
        }
    }

    fun getCode(name: String): Int {
        val id = sNameToIdMap[name] ?: throw RuntimeException("Unknown key code: $name")
        return DEFAULT[id]
    }
}
