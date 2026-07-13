/*
 * Copyright (C) 2014 The Android Open Source Project
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

import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.MoreKeysPanel
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker

interface DrawingProxy {
    fun onKeyPressed(key: Key, withPreview: Boolean)
    fun onKeyReleased(key: Key, withAnimation: Boolean)
    fun showMoreKeysKeyboard(key: Key, tracker: PointerTracker): MoreKeysPanel?
    fun startWhileTypingAnimation(fadeInOrOut: Int)

    companion object {
        const val FADE_IN = 0
        const val FADE_OUT = 1
    }
}
