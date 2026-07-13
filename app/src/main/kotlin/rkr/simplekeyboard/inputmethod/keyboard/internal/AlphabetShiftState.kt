/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2021 Raimondas Rimkus
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

class AlphabetShiftState {
    private var mState = UNSHIFTED

    fun setShifted(newShiftState: Boolean) {
        val oldState = mState
        if (newShiftState) {
            when (oldState) {
                UNSHIFTED -> mState = MANUAL_SHIFTED
                SHIFT_LOCKED -> mState = SHIFT_LOCK_SHIFTED
            }
        } else {
            when (oldState) {
                MANUAL_SHIFTED, AUTOMATIC_SHIFTED -> mState = UNSHIFTED
                SHIFT_LOCK_SHIFTED -> mState = SHIFT_LOCKED
            }
        }
        if (DEBUG) Log.d(TAG, "setShifted($newShiftState): ${toString(oldState)} > $this")
    }

    fun setShiftLocked(newShiftLockState: Boolean) {
        val oldState = mState
        if (newShiftLockState) {
            when (oldState) {
                UNSHIFTED, MANUAL_SHIFTED, AUTOMATIC_SHIFTED -> mState = SHIFT_LOCKED
            }
        } else {
            mState = UNSHIFTED
        }
        if (DEBUG) Log.d(TAG, "setShiftLocked($newShiftLockState): ${toString(oldState)} > $this")
    }

    fun setAutomaticShifted() {
        mState = AUTOMATIC_SHIFTED
    }

    fun isShiftedOrShiftLocked(): Boolean = mState != UNSHIFTED

    fun isShiftLocked(): Boolean = mState == SHIFT_LOCKED || mState == SHIFT_LOCK_SHIFTED

    fun isShiftLockShifted(): Boolean = mState == SHIFT_LOCK_SHIFTED

    fun isAutomaticShifted(): Boolean = mState == AUTOMATIC_SHIFTED

    fun isManualShifted(): Boolean = mState == MANUAL_SHIFTED || mState == SHIFT_LOCK_SHIFTED

    override fun toString(): String = toString(mState)

    companion object {
        private val TAG = AlphabetShiftState::class.java.simpleName
        private const val DEBUG = false

        private const val UNSHIFTED = 0
        private const val MANUAL_SHIFTED = 1
        private const val AUTOMATIC_SHIFTED = 2
        private const val SHIFT_LOCKED = 3
        private const val SHIFT_LOCK_SHIFTED = 4

        private fun toString(state: Int): String = when (state) {
            UNSHIFTED -> "UNSHIFTED"
            MANUAL_SHIFTED -> "MANUAL_SHIFTED"
            AUTOMATIC_SHIFTED -> "AUTOMATIC_SHIFTED"
            SHIFT_LOCKED -> "SHIFT_LOCKED"
            SHIFT_LOCK_SHIFTED -> "SHIFT_LOCK_SHIFTED"
            else -> "UNKNOWN"
        }
    }
}
