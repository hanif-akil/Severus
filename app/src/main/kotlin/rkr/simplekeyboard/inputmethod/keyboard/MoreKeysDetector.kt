/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2020 wittmane
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

package rkr.simplekeyboard.inputmethod.keyboard

class MoreKeysDetector(slideAllowance: Float) : KeyDetector() {
    private val mSlideAllowanceSquare: Int = (slideAllowance * slideAllowance).toInt()
    // Top slide allowance is slightly longer (sqrt(2) times) than other edges.
    private val mSlideAllowanceSquareTop: Int = mSlideAllowanceSquare * 2

    override fun alwaysAllowsKeySelectionByDraggingFinger(): Boolean = true

    override fun detectHitKey(x: Int, y: Int): Key? {
        val keyboard = getKeyboard() ?: return null
        val touchX = getTouchX(x)
        val touchY = getTouchY(y)

        var nearestKey: Key? = null
        var nearestDist = if (y < 0) mSlideAllowanceSquareTop else mSlideAllowanceSquare
        for (key in keyboard.getSortedKeys()) {
            val dist = key.squaredDistanceToHitboxEdge(touchX, touchY)
            if (dist < nearestDist) {
                nearestKey = key
                nearestDist = dist
            }
        }
        return nearestKey
    }
}
