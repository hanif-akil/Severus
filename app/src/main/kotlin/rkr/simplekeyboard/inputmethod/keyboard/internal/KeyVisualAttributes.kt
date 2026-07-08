package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.content.res.TypedArray
import android.graphics.Typeface
import android.util.SparseIntArray
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils

class KeyVisualAttributes private constructor(keyAttr: TypedArray) {
    val mTypeface: Typeface? = if (keyAttr.hasValue(R.styleable.Keyboard_Key_keyTypeface)) {
        Typeface.defaultFromStyle(keyAttr.getInt(R.styleable.Keyboard_Key_keyTypeface, Typeface.NORMAL))
    } else null

    val mLetterRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyLetterSize)
    val mLetterSize: Int = ResourceUtils.getDimensionPixelSize(keyAttr, R.styleable.Keyboard_Key_keyLetterSize)
    val mLabelRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyLabelSize)
    val mLabelSize: Int = ResourceUtils.getDimensionPixelSize(keyAttr, R.styleable.Keyboard_Key_keyLabelSize)
    val mLargeLetterRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyLargeLetterRatio)
    val mHintLetterRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyHintLetterRatio)
    val mShiftedLetterHintRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyShiftedLetterHintRatio)
    val mHintLabelRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyHintLabelRatio)
    val mPreviewTextRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyPreviewTextRatio)

    val mTextColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyTextColor, 0)
    val mTextInactivatedColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyTextInactivatedColor, 0)
    val mTextShadowColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyTextShadowColor, 0)
    val mFunctionalTextColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_functionalTextColor, 0)
    val mHintLetterColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyHintLetterColor, 0)
    val mHintLabelColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyHintLabelColor, 0)
    val mShiftedLetterHintInactivatedColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyShiftedLetterHintInactivatedColor, 0)
    val mShiftedLetterHintActivatedColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyShiftedLetterHintActivatedColor, 0)
    val mPreviewTextColor: Int = keyAttr.getColor(R.styleable.Keyboard_Key_keyPreviewTextColor, 0)

    val mHintLabelVerticalAdjustment: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyHintLabelVerticalAdjustment, 0.0f)
    val mLabelOffCenterRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyLabelOffCenterRatio, 0.0f)
    val mHintLabelOffCenterRatio: Float = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyHintLabelOffCenterRatio, 0.0f)

    companion object {
        private val VISUAL_ATTRIBUTE_IDS = intArrayOf(
            R.styleable.Keyboard_Key_keyTypeface,
            R.styleable.Keyboard_Key_keyLetterSize,
            R.styleable.Keyboard_Key_keyLabelSize,
            R.styleable.Keyboard_Key_keyLargeLetterRatio,
            R.styleable.Keyboard_Key_keyHintLetterRatio,
            R.styleable.Keyboard_Key_keyShiftedLetterHintRatio,
            R.styleable.Keyboard_Key_keyHintLabelRatio,
            R.styleable.Keyboard_Key_keyPreviewTextRatio,
            R.styleable.Keyboard_Key_keyTextColor,
            R.styleable.Keyboard_Key_keyTextInactivatedColor,
            R.styleable.Keyboard_Key_keyTextShadowColor,
            R.styleable.Keyboard_Key_functionalTextColor,
            R.styleable.Keyboard_Key_keyHintLetterColor,
            R.styleable.Keyboard_Key_keyHintLabelColor,
            R.styleable.Keyboard_Key_keyShiftedLetterHintInactivatedColor,
            R.styleable.Keyboard_Key_keyShiftedLetterHintActivatedColor,
            R.styleable.Keyboard_Key_keyPreviewTextColor,
            R.styleable.Keyboard_Key_keyHintLabelVerticalAdjustment,
            R.styleable.Keyboard_Key_keyLabelOffCenterRatio,
            R.styleable.Keyboard_Key_keyHintLabelOffCenterRatio
        )

        private val sVisualAttributeIds = SparseIntArray()
        private const val ATTR_DEFINED = 1
        private const val ATTR_NOT_FOUND = 0

        init {
            for (attrId in VISUAL_ATTRIBUTE_IDS) {
                sVisualAttributeIds.put(attrId, ATTR_DEFINED)
            }
        }

        fun newInstance(keyAttr: TypedArray): KeyVisualAttributes? {
            val indexCount = keyAttr.indexCount
            for (i in 0 until indexCount) {
                val attrId = keyAttr.getIndex(i)
                if (sVisualAttributeIds.get(attrId, ATTR_NOT_FOUND) == ATTR_NOT_FOUND) continue
                return KeyVisualAttributes(keyAttr)
            }
            return null
        }
    }
}
