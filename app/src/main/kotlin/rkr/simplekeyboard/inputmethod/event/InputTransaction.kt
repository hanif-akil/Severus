/*
 * Copyright (C) 2014 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.event

import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues

class InputTransaction(val mSettingsValues: SettingsValues) {

    companion object {
        const val SHIFT_NO_UPDATE = 0
        const val SHIFT_UPDATE_NOW = 1
        const val SHIFT_UPDATE_LATER = 2
    }

    private var mRequiredShiftUpdate = SHIFT_NO_UPDATE

    fun requireShiftUpdate(updateType: Int) {
        mRequiredShiftUpdate = maxOf(mRequiredShiftUpdate, updateType)
    }

    fun getRequiredShiftUpdate(): Int {
        return mRequiredShiftUpdate
    }
}
