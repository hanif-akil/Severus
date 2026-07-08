package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.util.TypedValue
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ResourceUtils {
    const val UNDEFINED_RATIO = -1.0f
    const val UNDEFINED_DIMENSION = -1

    fun getKeyboardHeight(res: Resources, settingsValues: SettingsValues): Int {
        val defaultKeyboardHeight = getDefaultKeyboardHeight(res)
        val scale = settingsValues.mKeyboardHeightScale
        return (defaultKeyboardHeight * scale).toInt()
    }

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
        return max(min(keyboardHeight, maxKeyboardHeight), minKeyboardHeight).toInt()
    }

    fun getKeyboardBottomOffset(res: Resources, settingsValues: SettingsValues): Int {
        return if (res.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            (settingsValues.mBottomOffsetPortrait * res.displayMetrics.density).toInt()
        } else {
            0
        }
    }

    fun isValidFraction(fraction: Float): Boolean = fraction >= 0.0f

    fun isValidDimensionPixelSize(dimension: Int): Boolean = dimension > 0

    fun getFraction(a: TypedArray, index: Int, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null || !isFractionValue(value)) return defValue
        return a.getFraction(index, 1, 1, defValue)
    }

    fun getFraction(a: TypedArray, index: Int): Float {
        return getFraction(a, index, UNDEFINED_RATIO)
    }

    fun getFraction(a: TypedArray, index: Int, base: Float, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null || !isFractionValue(value)) return defValue
        return value.getFraction(base, base)
    }

    fun getDimensionPixelSize(a: TypedArray, index: Int): Int {
        val value = a.peekValue(index)
        if (value == null || !isDimensionValue(value)) return UNDEFINED_DIMENSION
        return a.getDimensionPixelSize(index, UNDEFINED_DIMENSION)
    }

    fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Float): Float {
        val value = a.peekValue(index) ?: return defValue
        return when {
            isFractionValue(value) -> a.getFraction(index, base, base, defValue)
            isDimensionValue(value) -> a.getDimension(index, defValue)
            else -> defValue
        }
    }

    fun getDimensionOrFraction(a: TypedArray, index: Int, base: Float, defValue: Float): Float {
        val value = a.peekValue(index) ?: return defValue
        return when {
            isFractionValue(value) -> value.getFraction(index, base.toFloat())
            isDimensionValue(value) -> a.getDimension(index, defValue)
            else -> defValue
        }
    }

    fun getEnumValue(a: TypedArray, index: Int, defValue: Int): Int {
        val value = a.peekValue(index) ?: return defValue
        return if (isIntegerValue(value)) a.getInt(index, defValue) else defValue
    }

    fun isFractionValue(v: TypedValue): Boolean = v.type == TypedValue.TYPE_FRACTION

    fun isDimensionValue(v: TypedValue): Boolean = v.type == TypedValue.TYPE_DIMENSION

    fun isIntegerValue(v: TypedValue): Boolean =
        v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT

    fun isStringValue(v: TypedValue): Boolean = v.type == TypedValue.TYPE_STRING

    fun isBrightColor(color: Int): Boolean {
        if (android.R.color.transparent == color) return true
        val rgb = intArrayOf(Color.red(color), Color.green(color), Color.blue(color))
        val brightness = sqrt(
            rgb[0] * rgb[0] * .241 + rgb[1] * rgb[1] * .691 + rgb[2] * rgb[2] * .068
        ).toInt()
        return brightness >= 210
    }
}
