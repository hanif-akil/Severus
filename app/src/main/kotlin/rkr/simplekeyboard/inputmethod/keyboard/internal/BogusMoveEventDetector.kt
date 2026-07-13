/*
 * Copyright (C) 2013 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.define.DebugFlags

class BogusMoveEventDetector {
    private var mAccumulatedDistanceThreshold = 0
    var mAccumulatedDistanceFromDownKey = 0
        internal set
    private var mActualDownX = 0
    private var mActualDownY = 0

    fun setKeyboardGeometry(keyPaddedWidth: Int, keyPaddedHeight: Int) {
        val keyDiagonal = Math.hypot(keyPaddedWidth.toDouble(), keyPaddedHeight.toDouble()).toFloat()
        mAccumulatedDistanceThreshold = (keyDiagonal * BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD).toInt()
    }

    fun onActualDownEvent(x: Int, y: Int) {
        mActualDownX = x
        mActualDownY = y
    }

    fun onDownKey() {
        mAccumulatedDistanceFromDownKey = 0
    }

    fun onMoveKey(distance: Int) {
        mAccumulatedDistanceFromDownKey += distance
    }

    fun hasTraveledLongDistance(x: Int, y: Int): Boolean {
        if (!sNeedsProximateBogusDownMoveUpEventHack) return false
        val dx = Math.abs(x - mActualDownX)
        val dy = Math.abs(y - mActualDownY)
        return dx >= dy && mAccumulatedDistanceFromDownKey >= mAccumulatedDistanceThreshold
    }

    fun getAccumulatedDistanceFromDownKey(): Int = mAccumulatedDistanceFromDownKey

    companion object {
        private val TAG = BogusMoveEventDetector::class.java.simpleName
        private val DEBUG_MODE = DebugFlags.DEBUG_ENABLED

        private const val BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD = 0.53f
        private var sNeedsProximateBogusDownMoveUpEventHack = false

        @JvmStatic
        fun init(res: Resources) {
            val screenMetrics = res.getInteger(R.integer.config_screen_metrics)
            val isLargeTablet = screenMetrics == Constants.SCREEN_METRICS_LARGE_TABLET
            val isSmallTablet = screenMetrics == Constants.SCREEN_METRICS_SMALL_TABLET
            val densityDpi = res.displayMetrics.densityDpi
            val hasLowDensityScreen = densityDpi < DisplayMetrics.DENSITY_HIGH
            val needsTheHack = isLargeTablet || (isSmallTablet && hasLowDensityScreen)
            if (DEBUG_MODE) {
                val sw = res.configuration.smallestScreenWidthDp
                Log.d(TAG, "needsProximateBogusDownMoveUpEventHack=$needsTheHack smallestScreenWidthDp=$sw densityDpi=$densityDpi screenMetrics=$screenMetrics")
            }
            sNeedsProximateBogusDownMoveUpEventHack = needsTheHack
        }
    }
}
