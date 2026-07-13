/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.compat

import android.view.inputmethod.EditorInfo
import java.util.Locale

object EditorInfoCompatUtils {
    @JvmStatic
    fun imeActionName(imeOptions: Int): String {
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        return when (actionId) {
            EditorInfo.IME_ACTION_UNSPECIFIED -> "actionUnspecified"
            EditorInfo.IME_ACTION_NONE -> "actionNone"
            EditorInfo.IME_ACTION_GO -> "actionGo"
            EditorInfo.IME_ACTION_SEARCH -> "actionSearch"
            EditorInfo.IME_ACTION_SEND -> "actionSend"
            EditorInfo.IME_ACTION_NEXT -> "actionNext"
            EditorInfo.IME_ACTION_DONE -> "actionDone"
            EditorInfo.IME_ACTION_PREVIOUS -> "actionPrevious"
            else -> "actionUnknown($actionId)"
        }
    }

    @JvmStatic
    fun getPrimaryHintLocale(editorInfo: EditorInfo?): Locale? {
        editorInfo ?: return null
        val localeList = editorInfo.hintLocales
        return if (localeList != null && !localeList.isEmpty()) localeList[0] else null
    }
}
