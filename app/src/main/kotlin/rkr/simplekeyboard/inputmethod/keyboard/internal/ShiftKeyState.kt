/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2017 Raimondas Rimkus
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

import android.util.Log

internal class ShiftKeyState(name: String) : ModifierKeyState(name) {
    override fun onOtherKeyPressed() {
        val oldState = mState
        if (oldState == PRESSING) {
            mState = CHORDING
        } else if (oldState == PRESSING_ON_SHIFTED) {
            mState = IGNORING
        }
        if (DEBUG) Log.d(TAG, "$mName.onOtherKeyPressed: ${toString(oldState)} > $this")
    }

    fun onPressOnShifted() {
        mState = PRESSING_ON_SHIFTED
    }

    fun isPressingOnShifted(): Boolean = mState == PRESSING_ON_SHIFTED

    fun isIgnoring(): Boolean = mState == IGNORING

    override fun toString(): String = toString(mState)

    override fun toString(state: Int): String = when (state) {
        PRESSING_ON_SHIFTED -> "PRESSING_ON_SHIFTED"
        IGNORING -> "IGNORING"
        else -> super.toString(state)
    }

    companion object {
        private const val PRESSING_ON_SHIFTED = 3
        private const val IGNORING = 4
    }
}
