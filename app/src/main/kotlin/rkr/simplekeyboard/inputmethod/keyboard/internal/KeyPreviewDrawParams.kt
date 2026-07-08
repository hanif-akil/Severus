package rkr.simplekeyboard.inputmethod.keyboard.internal

class KeyPreviewDrawParams {
    var mPreviewHeight = 0
    var mPreviewWidth = 0
    var mPreviewPositionX = 0
    var mPreviewPositionY = 0
    var mPreviewGravity = 0
    var mDrawableGravity = 0
    var mDrawableWidth = 0
    var mDrawableHeight = 0
    var mShadowSize = 0
    var mShadowOffsetY = 0
    var mShadowColor = 0

    fun computeDimensions(keyDrawParams: KeyDrawParams, keyWidth: Int) {
        mPreviewHeight = keyDrawParams.mPreviewTextSize * 3 / 2
        mPreviewWidth = keyWidth * 2
        mDrawableWidth = mPreviewWidth - mShadowSize * 2
        mDrawableHeight = mPreviewHeight - mShadowSize * 2
    }
}
