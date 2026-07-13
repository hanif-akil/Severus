/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2020 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.latin.utils

import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager

object LanguageOnSpacebarUtils {
    const val FORMAT_TYPE_NONE = 0
    const val FORMAT_TYPE_LANGUAGE_ONLY = 1
    const val FORMAT_TYPE_FULL_LOCALE = 2

    @JvmStatic
    fun getLanguageOnSpacebarFormatType(subtype: Subtype): Int {
        val locale = subtype.localeObject
        if (locale == null) {
            return FORMAT_TYPE_NONE
        }
        val keyboardLanguage = locale.language
        val keyboardLayout = subtype.keyboardLayoutSet
        var sameLanguageAndLayoutCount = 0
        val enabledSubtypes = RichInputMethodManager.getInstance().getEnabledSubtypes(false)
        for (enabledSubtype in enabledSubtypes) {
            val language = enabledSubtype.localeObject.language
            if (keyboardLanguage == language
                && keyboardLayout == enabledSubtype.keyboardLayoutSet
            ) {
                sameLanguageAndLayoutCount++
            }
        }
        return if (sameLanguageAndLayoutCount > 1) FORMAT_TYPE_FULL_LOCALE
        else FORMAT_TYPE_LANGUAGE_ONLY
    }
}
