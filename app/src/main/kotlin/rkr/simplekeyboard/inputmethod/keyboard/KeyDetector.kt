/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2020 wittmane
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

package rkr.simplekeyboard.inputmethod.keyboard

/**
 * This class handles key detection.
 */
open class KeyDetector {
    private val mKeyHysteresisDistanceSquared: Int
    private val mKeyHysteresisDistanceForSlidingModifierSquared: Int

    private var mKeyboard: Keyboard? = null
    private var mCorrectionX: Int = 0
    private var mCorrectionY: Int = 0

    constructor() : this(0.0f, 0.0f)

    /**
     * Key detection object constructor with key hysteresis distances.
     */
    constructor(keyHysteresisDistance: Float, keyHysteresisDistanceForSlidingModifier: Float) {
        mKeyHysteresisDistanceSquared = (keyHysteresisDistance * keyHysteresisDistance).toInt()
        mKeyHysteresisDistanceForSlidingModifierSquared =
            (keyHysteresisDistanceForSlidingModifier * keyHysteresisDistanceForSlidingModifier).toInt()
    }

    fun setKeyboard(keyboard: Keyboard, correctionX: Float, correctionY: Float) {
        mCorrectionX = correctionX.toInt()
        mCorrectionY = correctionY.toInt()
        mKeyboard = keyboard
    }

    fun getKeyHysteresisDistanceSquared(isSlidingFromModifier: Boolean): Int {
        return if (isSlidingFromModifier) mKeyHysteresisDistanceForSlidingModifierSquared
        else mKeyHysteresisDistanceSquared
    }

    fun getTouchX(x: Int): Int = x + mCorrectionX

    // TODO: Remove vertical correction.
    fun getTouchY(y: Int): Int = y + mCorrectionY

    fun getKeyboard(): Keyboard? = mKeyboard

    open fun alwaysAllowsKeySelectionByDraggingFinger(): Boolean = false

    /**
     * Detect the key whose hitbox the touch point is in.
     */
    open fun detectHitKey(x: Int, y: Int): Key? {
        val keyboard = mKeyboard ?: return null
        val touchX = getTouchX(x)
        val touchY = getTouchY(y)

        for (key in keyboard.getNearestKeys(touchX, touchY)) {
            if (key.isOnKey(touchX, touchY)) {
                return key
            }
        }
        return null
    }
}
