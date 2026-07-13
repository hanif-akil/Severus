/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2019 Emmanuel
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

package rkr.simplekeyboard.inputmethod.keyboard

import rkr.simplekeyboard.inputmethod.latin.common.Constants

interface KeyboardActionListener {
    /**
     * Called when the user presses a key. This is sent before the [onCodeInput] is called.
     * For keys that repeat, this is only called once.
     */
    fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean)

    /**
     * Called when the user releases a key. This is sent after the [onCodeInput] is called.
     * For keys that repeat, this is only called once.
     */
    fun onReleaseKey(primaryCode: Int, withSliding: Boolean)

    /**
     * Send a key code to the listener.
     */
    fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean)

    /**
     * Sends a string of characters to the listener.
     */
    fun onTextInput(rawText: String)

    /**
     * Called when user finished sliding key input.
     */
    fun onFinishSlidingInput()

    /**
     * Send a non-"code input" custom request to the listener.
     * @return true if the request has been consumed, false otherwise.
     */
    fun onCustomRequest(requestCode: Int): Boolean

    fun onMoveCursorPointer(steps: Int)
    fun onMoveDeletePointer(steps: Int)
    fun onUpWithDeletePointerActive()
    fun onUpWithSpacePointerActive()

    companion object {
        @JvmField
        val EMPTY_LISTENER: KeyboardActionListener = object : KeyboardActionListener {
            override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean) {}
            override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {}
            override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {}
            override fun onTextInput(text: String) {}
            override fun onFinishSlidingInput() {}
            override fun onCustomRequest(requestCode: Int): Boolean = false
            override fun onMoveCursorPointer(steps: Int) {}
            override fun onMoveDeletePointer(steps: Int) {}
            override fun onUpWithDeletePointerActive() {}
            override fun onUpWithSpacePointerActive() {}
        }
    }
}
