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

open class ModifierKeyState(protected val mName: String) {
    protected var mState = RELEASING

    fun onPress() {
        mState = PRESSING
    }

    fun onRelease() {
        mState = RELEASING
    }

    open fun onOtherKeyPressed() {
        val oldState = mState
        if (oldState == PRESSING) mState = CHORDING
        if (DEBUG) Log.d(TAG, "$mName.onOtherKeyPressed: ${toString(oldState)} > $this")
    }

    fun isPressing(): Boolean = mState == PRESSING

    fun isReleasing(): Boolean = mState == RELEASING

    fun isChording(): Boolean = mState == CHORDING

    override fun toString(): String = toString(mState)

    protected open fun toString(state: Int): String = when (state) {
        RELEASING -> "RELEASING"
        PRESSING -> "PRESSING"
        CHORDING -> "CHORDING"
        else -> "UNKNOWN"
    }

    companion object {
        protected val TAG = ModifierKeyState::class.java.simpleName
        protected const val DEBUG = false

        protected const val RELEASING = 0
        protected const val PRESSING = 1
        protected const val CHORDING = 2
    }
}
