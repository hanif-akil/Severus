/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2024 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
 * Copyright (C) 2019 Micha LaQua
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

package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.util.TypedValue
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues

object ResourceUtils {
    const val UNDEFINED_RATIO: Float = -1.0f
    const val UNDEFINED_DIMENSION: Int = -1

    @JvmStatic
    fun getKeyboardHeight(res: Resources, settingsValues: SettingsValues): Int {
        val defaultKeyboardHeight = getDefaultKeyboardHeight(res)
        val scale = settingsValues.mKeyboardHeightScale
        return (defaultKeyboardHeight * scale).toInt()
    }

    @JvmStatic
    fun getDefaultKeyboardHeight(res: Resources): Int {
        val dm = res.displayMetrics
        val keyboardHeight = res.getDimension(R.dimen.config_default_keyboard_height)
        val maxKeyboardHeight = res.getFraction(
            R.fraction.config_max_keyboard_height, dm.heightPixels, dm.heightPixels
        )
        var minKeyboardHeight = res.getFraction(
            R.fraction.config_min_keyboard_height, dm.heightPixels, dm.heightPixels
        )
        if (minKeyboardHeight < 0.0f) {
            minKeyboardHeight = -res.getFraction(
                R.fraction.config_min_keyboard_height, dm.widthPixels, dm.widthPixels
            )
        }
        return Math.max(Math.min(keyboardHeight, maxKeyboardHeight), minKeyboardHeight).toInt()
    }

    @JvmStatic
    fun getKeyboardBottomOffset(res: Resources, settingsValues: SettingsValues): Int {
        return if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            (settingsValues.mBottomOffsetPortrait * res.displayMetrics.density).toInt()
        } else {
            0
        }
    }

    @JvmStatic
    fun isValidFraction(fraction: Float): Boolean {
        return fraction >= 0.0f
    }

    @JvmStatic
    fun isValidDimensionPixelSize(dimension: Int): Boolean {
        return dimension > 0
    }

    @JvmStatic
    fun getFraction(a: TypedArray, index: Int, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null || !isFractionValue(value)) {
            return defValue
        }
        return a.getFraction(index, 1, 1, defValue)
    }

    @JvmStatic
    fun getFraction(a: TypedArray, index: Int): Float {
        return getFraction(a, index, UNDEFINED_RATIO)
    }

    @JvmStatic
    fun getFraction(a: TypedArray, index: Int, base: Float, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null || !isFractionValue(value)) {
            return defValue
        }
        return value.getFraction(base, base)
    }

    @JvmStatic
    fun getDimensionPixelSize(a: TypedArray, index: Int): Int {
        val value = a.peekValue(index)
        if (value == null || !isDimensionValue(value)) {
            return UNDEFINED_DIMENSION
        }
        return a.getDimensionPixelSize(index, UNDEFINED_DIMENSION)
    }

    @JvmStatic
    fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null) {
            return defValue
        }
        if (isFractionValue(value)) {
            return a.getFraction(index, base, base, defValue)
        } else if (isDimensionValue(value)) {
            return a.getDimension(index, defValue)
        }
        return defValue
    }

    @JvmStatic
    fun getDimensionOrFraction(a: TypedArray, index: Int, base: Float, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null) {
            return defValue
        }
        if (isFractionValue(value)) {
            return value.getFraction(index, base)
        } else if (isDimensionValue(value)) {
            return a.getDimension(index, defValue)
        }
        return defValue
    }

    @JvmStatic
    fun getEnumValue(a: TypedArray, index: Int, defValue: Int): Int {
        val value = a.peekValue(index)
        if (value == null) {
            return defValue
        }
        if (isIntegerValue(value)) {
            return a.getInt(index, defValue)
        }
        return defValue
    }

    @JvmStatic
    fun isFractionValue(v: TypedValue): Boolean {
        return v.type == TypedValue.TYPE_FRACTION
    }

    @JvmStatic
    fun isDimensionValue(v: TypedValue): Boolean {
        return v.type == TypedValue.TYPE_DIMENSION
    }

    @JvmStatic
    fun isIntegerValue(v: TypedValue): Boolean {
        return v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT
    }

    @JvmStatic
    fun isStringValue(v: TypedValue): Boolean {
        return v.type == TypedValue.TYPE_STRING
    }

    @JvmStatic
    fun isBrightColor(color: Int): Boolean {
        if (android.R.color.transparent == color) {
            return true
        }
        val rgb = intArrayOf(Color.red(color), Color.green(color), Color.blue(color))
        val brightness = Math.sqrt(
            rgb[0] * rgb[0] * .241 + rgb[1] * rgb[1] * .691 + rgb[2] * rgb[2] * .068
        ).toInt()
        return brightness >= 210
    }
}
