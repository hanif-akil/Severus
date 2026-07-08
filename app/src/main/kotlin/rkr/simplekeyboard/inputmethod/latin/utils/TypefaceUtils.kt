package rkr.simplekeyboard.inputmethod.latin.utils

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.SparseArray

object TypefaceUtils {
    private val KEY_LABEL_REFERENCE_CHAR = charArrayOf('M')
    private val KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR = charArrayOf('8')

    private val sTextHeightCache = SparseArray<Float>()
    private val sTextHeightBounds = Rect()

    private fun getCharHeight(referenceChar: CharArray, paint: Paint): Float {
        val key = getCharGeometryCacheKey(referenceChar[0], paint)
        synchronized(sTextHeightCache) {
            val cachedValue = sTextHeightCache.get(key)
            if (cachedValue != null) return cachedValue
            paint.getTextBounds(referenceChar, 0, 1, sTextHeightBounds)
            val height = sTextHeightBounds.height().toFloat()
            sTextHeightCache.put(key, height)
            return height
        }
    }

    private val sTextWidthCache = SparseArray<Float>()
    private val sTextWidthBounds = Rect()

    private fun getCharWidth(referenceChar: CharArray, paint: Paint): Float {
        val key = getCharGeometryCacheKey(referenceChar[0], paint)
        synchronized(sTextWidthCache) {
            val cachedValue = sTextWidthCache.get(key)
            if (cachedValue != null) return cachedValue
            paint.getTextBounds(referenceChar, 0, 1, sTextWidthBounds)
            val width = sTextWidthBounds.width().toFloat()
            sTextWidthCache.put(key, width)
            return width
        }
    }

    private fun getCharGeometryCacheKey(referenceChar: Char, paint: Paint): Int {
        val labelSize = paint.textSize.toInt()
        val face = paint.typeface
        val codePointOffset = referenceChar.code shl 15
        return when (face) {
            Typeface.DEFAULT -> codePointOffset + labelSize
            Typeface.DEFAULT_BOLD -> codePointOffset + labelSize + 0x1000
            Typeface.MONOSPACE -> codePointOffset + labelSize + 0x2000
            else -> codePointOffset + labelSize
        }
    }

    fun getReferenceCharHeight(paint: Paint): Float {
        return getCharHeight(KEY_LABEL_REFERENCE_CHAR, paint)
    }

    fun getReferenceCharWidth(paint: Paint): Float {
        return getCharWidth(KEY_LABEL_REFERENCE_CHAR, paint)
    }

    fun getReferenceDigitWidth(paint: Paint): Float {
        return getCharWidth(KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR, paint)
    }

    private val sStringWidthBounds = Rect()

    fun getStringWidth(string: String, paint: Paint): Float {
        synchronized(sStringWidthBounds) {
            paint.getTextBounds(string, 0, string.length, sStringWidthBounds)
            return sStringWidthBounds.width().toFloat()
        }
    }
}
