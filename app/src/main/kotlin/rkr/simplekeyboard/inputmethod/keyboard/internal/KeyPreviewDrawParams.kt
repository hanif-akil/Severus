/*
 * Copyright (C) 2012 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.res.TypedArray
import android.view.View
import android.view.animation.AccelerateInterpolator
import rkr.simplekeyboard.inputmethod.R

class KeyPreviewDrawParams(mainKeyboardViewAttr: TypedArray) {
    val mPreviewOffset: Int = mainKeyboardViewAttr.getDimensionPixelOffset(R.styleable.MainKeyboardView_keyPreviewOffset, 0)
    val mPreviewHeight: Int = mainKeyboardViewAttr.getDimensionPixelSize(R.styleable.MainKeyboardView_keyPreviewHeight, 0)
    val mMinPreviewWidth: Int = mainKeyboardViewAttr.getDimensionPixelSize(R.styleable.MainKeyboardView_keyPreviewWidth, 0)
    val mPreviewBackgroundResId: Int = mainKeyboardViewAttr.getResourceId(R.styleable.MainKeyboardView_keyPreviewBackground, 0)
    private val mDismissAnimatorResId: Int = mainKeyboardViewAttr.getResourceId(R.styleable.MainKeyboardView_keyPreviewDismissAnimator, 0)
    private var mLingerTimeout: Int = mainKeyboardViewAttr.getInt(R.styleable.MainKeyboardView_keyPreviewLingerTimeout, 0)
    private var mShowPopup: Boolean = true

    private var mVisibleWidth = 0
    private var mVisibleHeight = 0
    private var mVisibleOffset = 0

    fun setVisibleOffset(previewVisibleOffset: Int) {
        mVisibleOffset = previewVisibleOffset
    }

    fun getVisibleOffset(): Int = mVisibleOffset

    fun setGeometry(previewTextView: View) {
        val previewWidth = Math.max(previewTextView.measuredWidth, mMinPreviewWidth)
        mVisibleWidth = previewWidth - previewTextView.paddingLeft - previewTextView.paddingRight
        mVisibleHeight = mPreviewHeight - previewTextView.paddingTop - previewTextView.paddingBottom
        setVisibleOffset(mPreviewOffset - previewTextView.paddingBottom)
    }

    fun getVisibleWidth(): Int = mVisibleWidth
    fun getVisibleHeight(): Int = mVisibleHeight

    fun setPopupEnabled(enabled: Boolean, lingerTimeout: Int) {
        mShowPopup = enabled
        mLingerTimeout = lingerTimeout
    }

    fun isPopupEnabled(): Boolean = mShowPopup
    fun getLingerTimeout(): Int = mLingerTimeout

    fun createDismissAnimator(target: View): Animator {
        val animator = AnimatorInflater.loadAnimator(target.context, mDismissAnimatorResId)
        animator.setTarget(target)
        animator.interpolator = ACCELERATE_INTERPOLATOR
        return animator
    }

    companion object {
        private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator()
    }
}
