package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.graphics.Typeface
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils

class KeyDrawParams {
    var mTypeface: Typeface = Typeface.DEFAULT
    var mLetterSize = 0
    var mLabelSize = 0
    var mLargeLetterSize = 0
    var mHintLetterSize = 0
    var mShiftedLetterHintSize = 0
    var mHintLabelSize = 0
    var mPreviewTextSize = 0
    var mTextColor = 0
    var mTextInactivatedColor = 0
    var mTextShadowColor = 0
    var mFunctionalTextColor = 0
    var mHintLetterColor = 0
    var mHintLabelColor = 0
    var mShiftedLetterHintInactivatedColor = 0
    var mShiftedLetterHintActivatedColor = 0
    var mPreviewTextColor = 0
    var mHintLabelVerticalAdjustment = 0f
    var mLabelOffCenterRatio = 0f
    var mHintLabelOffCenterRatio = 0f
    var mAnimAlpha = 0

    constructor()

    private constructor(copyFrom: KeyDrawParams) {
        mTypeface = copyFrom.mTypeface
        mLetterSize = copyFrom.mLetterSize
        mLabelSize = copyFrom.mLabelSize
        mLargeLetterSize = copyFrom.mLargeLetterSize
        mHintLetterSize = copyFrom.mHintLetterSize
        mShiftedLetterHintSize = copyFrom.mShiftedLetterHintSize
        mHintLabelSize = copyFrom.mHintLabelSize
        mPreviewTextSize = copyFrom.mPreviewTextSize
        mTextColor = copyFrom.mTextColor
        mTextInactivatedColor = copyFrom.mTextInactivatedColor
        mTextShadowColor = copyFrom.mTextShadowColor
        mFunctionalTextColor = copyFrom.mFunctionalTextColor
        mHintLetterColor = copyFrom.mHintLetterColor
        mHintLabelColor = copyFrom.mHintLabelColor
        mShiftedLetterHintInactivatedColor = copyFrom.mShiftedLetterHintInactivatedColor
        mShiftedLetterHintActivatedColor = copyFrom.mShiftedLetterHintActivatedColor
        mPreviewTextColor = copyFrom.mPreviewTextColor
        mHintLabelVerticalAdjustment = copyFrom.mHintLabelVerticalAdjustment
        mLabelOffCenterRatio = copyFrom.mLabelOffCenterRatio
        mHintLabelOffCenterRatio = copyFrom.mHintLabelOffCenterRatio
        mAnimAlpha = copyFrom.mAnimAlpha
    }

    fun updateParams(keyHeight: Int, attr: KeyVisualAttributes?) {
        if (attr == null) return
        if (attr.mTypeface != null) mTypeface = attr.mTypeface!!

        mLetterSize = selectTextSizeFromDimensionOrRatio(keyHeight, attr.mLetterSize, attr.mLetterRatio, mLetterSize)
        mLabelSize = selectTextSizeFromDimensionOrRatio(keyHeight, attr.mLabelSize, attr.mLabelRatio, mLabelSize)
        mLargeLetterSize = selectTextSize(keyHeight, attr.mLargeLetterRatio, mLargeLetterSize)
        mHintLetterSize = selectTextSize(keyHeight, attr.mHintLetterRatio, mHintLetterSize)
        mShiftedLetterHintSize = selectTextSize(keyHeight, attr.mShiftedLetterHintRatio, mShiftedLetterHintSize)
        mHintLabelSize = selectTextSize(keyHeight, attr.mHintLabelRatio, mHintLabelSize)
        mPreviewTextSize = selectTextSize(keyHeight, attr.mPreviewTextRatio, mPreviewTextSize)

        mTextColor = selectColor(attr.mTextColor, mTextColor)
        mTextInactivatedColor = selectColor(attr.mTextInactivatedColor, mTextInactivatedColor)
        mTextShadowColor = selectColor(attr.mTextShadowColor, mTextShadowColor)
        mFunctionalTextColor = selectColor(attr.mFunctionalTextColor, mFunctionalTextColor)
        mHintLetterColor = selectColor(attr.mHintLetterColor, mHintLetterColor)
        mHintLabelColor = selectColor(attr.mHintLabelColor, mHintLabelColor)
        mShiftedLetterHintInactivatedColor = selectColor(attr.mShiftedLetterHintInactivatedColor, mShiftedLetterHintInactivatedColor)
        mShiftedLetterHintActivatedColor = selectColor(attr.mShiftedLetterHintActivatedColor, mShiftedLetterHintActivatedColor)
        mPreviewTextColor = selectColor(attr.mPreviewTextColor, mPreviewTextColor)

        mHintLabelVerticalAdjustment = selectFloatIfNonZero(attr.mHintLabelVerticalAdjustment, mHintLabelVerticalAdjustment)
        mLabelOffCenterRatio = selectFloatIfNonZero(attr.mLabelOffCenterRatio, mLabelOffCenterRatio)
        mHintLabelOffCenterRatio = selectFloatIfNonZero(attr.mHintLabelOffCenterRatio, mHintLabelOffCenterRatio)
    }

    fun mayCloneAndUpdateParams(keyHeight: Int, attr: KeyVisualAttributes?): KeyDrawParams {
        if (attr == null) return this
        val newParams = KeyDrawParams(this)
        newParams.updateParams(keyHeight, attr)
        return newParams
    }

    companion object {
        private fun selectTextSizeFromDimensionOrRatio(keyHeight: Int, dimens: Int, ratio: Float, defaultDimens: Int): Int {
            if (ResourceUtils.isValidDimensionPixelSize(dimens)) return dimens
            if (ResourceUtils.isValidFraction(ratio)) return (keyHeight * ratio).toInt()
            return defaultDimens
        }

        private fun selectTextSize(keyHeight: Int, ratio: Float, defaultSize: Int): Int {
            if (ResourceUtils.isValidFraction(ratio)) return (keyHeight * ratio).toInt()
            return defaultSize
        }

        private fun selectColor(attrColor: Int, defaultColor: Int): Int = if (attrColor != 0) attrColor else defaultColor

        private fun selectFloatIfNonZero(attrFloat: Float, defaultFloat: Float): Float = if (attrFloat != 0f) attrFloat else defaultFloat
    }
}
