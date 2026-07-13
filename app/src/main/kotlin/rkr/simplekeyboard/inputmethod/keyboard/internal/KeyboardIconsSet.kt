/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseIntArray
import java.util.HashMap
import rkr.simplekeyboard.inputmethod.R

class KeyboardIconsSet {
    private val mIcons = arrayOfNulls<Drawable>(NUM_ICONS)

    fun loadIcons(resources: Resources, theme: Resources.Theme) {
        val size = ATTR_ID_TO_ICON_ID.size()
        for (index in 0 until size) {
            val attrId = ATTR_ID_TO_ICON_ID.keyAt(index)
            try {
                val icon = resources.getDrawable(attrId, theme)
                setDefaultBounds(icon)
                val iconId = ATTR_ID_TO_ICON_ID.get(attrId)
                mIcons[iconId] = icon
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "Drawable resource for icon #" + resources.getResourceEntryName(attrId) + " not found")
            }
        }
    }

    fun getIconDrawable(iconId: Int): Drawable? {
        if (isValidIconId(iconId)) return mIcons[iconId]
        throw RuntimeException("unknown icon id: ${getIconName(iconId)}")
    }

    companion object {
        private val TAG = KeyboardIconsSet::class.java.simpleName

        const val PREFIX_ICON = "!icon/"
        const val ICON_UNDEFINED = 0
        private const val ATTR_UNDEFINED = 0

        private const val NAME_UNDEFINED = "undefined"
        const val NAME_SHIFT_KEY = "shift_key"
        const val NAME_SHIFT_KEY_SHIFTED = "shift_key_shifted"
        const val NAME_DELETE_KEY = "delete_key"
        const val NAME_SETTINGS_KEY = "settings_key"
        const val NAME_PASTE_KEY = "paste_key"
        const val NAME_SPACE_KEY = "space_key"
        const val NAME_SPACE_KEY_FOR_NUMBER_LAYOUT = "space_key_for_number_layout"
        const val NAME_ENTER_KEY = "enter_key"
        const val NAME_GO_KEY = "go_key"
        const val NAME_SEARCH_KEY = "search_key"
        const val NAME_SEND_KEY = "send_key"
        const val NAME_NEXT_KEY = "next_key"
        const val NAME_DONE_KEY = "done_key"
        const val NAME_PREVIOUS_KEY = "previous_key"
        const val NAME_TAB_KEY = "tab_key"
        const val NAME_LANGUAGE_SWITCH_KEY = "language_switch_key"
        const val NAME_ZWNJ_KEY = "zwnj_key"
        const val NAME_ZWJ_KEY = "zwj_key"
        const val NAME_CLIPBOARD_KEY = "clipboard_key"
        const val NAME_NUMPAD_KEY = "numpad_key"

        private val ATTR_ID_TO_ICON_ID = SparseIntArray()

        private val sNameToIdsMap = HashMap<String, Int>()

        private val NAMES_AND_ATTR_IDS = arrayOf(
            NAME_UNDEFINED,                   ATTR_UNDEFINED,
            NAME_SHIFT_KEY,                   R.drawable.sym_keyboard_shift,
            NAME_DELETE_KEY,                  R.drawable.sym_keyboard_delete,
            NAME_SETTINGS_KEY,                R.drawable.sym_keyboard_settings,
            NAME_PASTE_KEY,                   R.drawable.sym_keyboard_paste,
            NAME_SPACE_KEY,                   ATTR_UNDEFINED,
            NAME_ENTER_KEY,                   R.drawable.sym_keyboard_return,
            NAME_GO_KEY,                      R.drawable.sym_keyboard_go,
            NAME_SEARCH_KEY,                  R.drawable.sym_keyboard_search,
            NAME_SEND_KEY,                    R.drawable.sym_keyboard_send,
            NAME_NEXT_KEY,                    R.drawable.sym_keyboard_next,
            NAME_DONE_KEY,                    R.drawable.sym_keyboard_done,
            NAME_PREVIOUS_KEY,                R.drawable.sym_keyboard_previous,
            NAME_TAB_KEY,                     R.drawable.sym_keyboard_tab,
            NAME_SPACE_KEY_FOR_NUMBER_LAYOUT, R.drawable.sym_keyboard_space,
            NAME_SHIFT_KEY_SHIFTED,           R.drawable.sym_keyboard_shift_locked,
            NAME_LANGUAGE_SWITCH_KEY,         R.drawable.sym_keyboard_language_switch,
            NAME_ZWNJ_KEY,                    R.drawable.sym_keyboard_zwnj,
            NAME_ZWJ_KEY,                     R.drawable.sym_keyboard_zwj,
            NAME_CLIPBOARD_KEY,               R.drawable.sym_keyboard_clipboard,
            NAME_NUMPAD_KEY,                   R.drawable.sym_keyboard_numpad
        )

        private val NUM_ICONS = NAMES_AND_ATTR_IDS.size / 2
        private val ICON_NAMES = arrayOfNulls<String>(NUM_ICONS)

        init {
            var iconId = ICON_UNDEFINED
            var i = 0
            while (i < NAMES_AND_ATTR_IDS.size) {
                val name = NAMES_AND_ATTR_IDS[i] as String
                val attrId = NAMES_AND_ATTR_IDS[i + 1] as Int
                if (attrId != ATTR_UNDEFINED) {
                    ATTR_ID_TO_ICON_ID.put(attrId, iconId)
                }
                sNameToIdsMap[name] = iconId
                ICON_NAMES[iconId] = name
                iconId++
                i += 2
            }
        }

        private fun isValidIconId(iconId: Int): Boolean {
            return iconId in ICON_NAMES.indices
        }

        @JvmStatic
        fun getIconName(iconId: Int): String {
            return if (isValidIconId(iconId)) ICON_NAMES[iconId]!! else "unknown<$iconId>"
        }

        @JvmStatic
        fun getIconId(name: String): Int {
            val iconId = sNameToIdsMap[name]
            if (iconId != null) return iconId
            throw RuntimeException("unknown icon name: $name")
        }

        private fun setDefaultBounds(icon: Drawable?) {
            if (icon != null) {
                icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            }
        }
    }
}
