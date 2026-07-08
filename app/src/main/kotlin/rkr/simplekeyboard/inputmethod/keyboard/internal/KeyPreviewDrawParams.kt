package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.res.TypedArray
import android.view.View
import rkr.simplekeyboard.inputmethod.R

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
    var mPreviewBackgroundResId = 0
    var mPreviewOffset = 0
    var mMinPreviewWidth = 0

    private var mPopupEnabled = true
    private var mPopupDismissDelay = 0

    constructor()

    constructor(keyboardViewAttr: TypedArray) {
        mPopupEnabled = true
        mPopupDismissDelay = keyboardViewAttr.getInt(R.styleable.MainKeyboardView_keyPreviewLingerTimeout, 0)
        mPreviewOffset = keyboardViewAttr.getDimensionPixelOffset(R.styleable.MainKeyboardView_keyPreviewOffset, 0)
        mMinPreviewWidth = keyboardViewAttr.getDimensionPixelSize(R.styleable.MainKeyboardView_keyPreviewWidth, 0)
        mPreviewBackgroundResId = keyboardViewAttr.getResourceId(R.styleable.MainKeyboardView_keyPreviewBackground, 0)
        mPreviewHeight = keyboardViewAttr.getDimensionPixelSize(R.styleable.MainKeyboardView_keyPreviewHeight, 0)
    }

    fun setPopupEnabled(popupEnabled: Boolean, delay: Int) {
        mPopupEnabled = popupEnabled
        mPopupDismissDelay = delay
    }

    fun isPopupEnabled(): Boolean = mPopupEnabled

    fun getVisibleWidth(): Int = mDrawableWidth

    fun getVisibleHeight(): Int = mDrawableHeight

    fun getLingerTimeout(): Int = mPopupDismissDelay

    fun getVisibleOffset(): Int = mPreviewOffset

    fun computeDimensions(keyDrawParams: KeyDrawParams, keyWidth: Int) {
        mPreviewHeight = keyDrawParams.mPreviewTextSize * 3 / 2
        mPreviewWidth = keyWidth * 2
        mDrawableWidth = mPreviewWidth - mShadowSize * 2
        mDrawableHeight = mPreviewHeight - mShadowSize * 2
    }

    fun setGeometry(keyPreviewView: KeyPreviewView) {
        val bg = keyPreviewView.background
        if (bg != null) {
            val padding = android.graphics.Rect()
            bg.getPadding(padding)
            mPreviewHeight = keyPreviewView.measuredHeight + padding.top + padding.bottom
            mPreviewWidth = keyPreviewView.measuredWidth + padding.left + padding.right
            mDrawableWidth = keyPreviewView.measuredWidth
            mDrawableHeight = keyPreviewView.measuredHeight
            mShadowSize = padding.top
            mShadowOffsetY = padding.bottom
        }
    }

    fun createDismissAnimator(keyPreviewView: KeyPreviewView): Animator {
        return ObjectAnimator.ofFloat(keyPreviewView, View.ALPHA, 1f, 0f).setDuration(DISMISS_ANIMATION_DURATION.toLong())
    }

    companion object {
        private const val DISMISS_ANIMATION_DURATION = 100
    }
}
