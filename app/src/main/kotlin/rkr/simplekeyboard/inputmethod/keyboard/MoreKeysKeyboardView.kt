/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
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

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils

/**
 * A view that renders a virtual [MoreKeysKeyboard]. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
open class MoreKeysKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyle: Int = R.attr.moreKeysKeyboardViewStyle
) : KeyboardView(context, attrs, defStyle), MoreKeysPanel {

    private val mCoordinates = CoordinateUtils.newInstance()

    protected val mKeyDetector: KeyDetector
    private var mController: MoreKeysPanel.Controller = MoreKeysPanel.EMPTY_CONTROLLER
    protected var mListener: KeyboardActionListener? = null
    private var mOriginX: Int = 0
    private var mOriginY: Int = 0
    private var mCurrentKey: Key? = null
    private var mActivePointerId: Int = 0

    init {
        val moreKeysKeyboardViewAttr = context.obtainStyledAttributes(
            attrs, R.styleable.MoreKeysKeyboardView, defStyle, R.style.MoreKeysKeyboardView
        )
        moreKeysKeyboardViewAttr.recycle()
        mKeyDetector = MoreKeysDetector(
            resources.getDimension(R.dimen.config_more_keys_keyboard_slide_allowance)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val keyboard = getKeyboard()
        if (keyboard != null) {
            val width = keyboard.mOccupiedWidth + paddingLeft + paddingRight
            val height = keyboard.mOccupiedHeight + paddingTop + paddingBottom
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun setKeyboard(keyboard: Keyboard) {
        super.setKeyboard(keyboard)
        mKeyDetector.setKeyboard(
            keyboard, -paddingLeft.toFloat(), (-paddingTop + verticalCorrection).toFloat()
        )
    }

    override fun showMoreKeysPanel(
        parentView: View,
        controller: MoreKeysPanel.Controller,
        pointX: Int,
        pointY: Int,
        listener: KeyboardActionListener
    ) {
        mController = controller
        mListener = listener
        val container = containerView
        val x = pointX - getDefaultCoordX() - container.paddingLeft - paddingLeft
        val y = pointY - container.measuredHeight + container.paddingBottom + paddingBottom

        parentView.getLocationInWindow(mCoordinates)
        val maxX = parentView.measuredWidth - container.measuredWidth
        val panelX = Math.max(0, Math.min(maxX, x)) + CoordinateUtils.x(mCoordinates)
        val panelY = y + CoordinateUtils.y(mCoordinates)
        container.x = panelX.toFloat()
        container.y = panelY.toFloat()

        mOriginX = x + container.paddingLeft
        mOriginY = y + container.paddingTop
        controller.onShowMoreKeysPanel(this)
    }

    protected fun getDefaultCoordX(): Int {
        @Suppress("UNCHECKED_CAST")
        return (getKeyboard() as MoreKeysKeyboard).getDefaultCoordX()
    }

    override fun onDownEvent(x: Int, y: Int, pointerId: Int) {
        mActivePointerId = pointerId
        mCurrentKey = detectKey(x, y)
    }

    override fun onMoveEvent(x: Int, y: Int, pointerId: Int) {
        if (mActivePointerId != pointerId) return
        val hasOldKey = mCurrentKey != null
        mCurrentKey = detectKey(x, y)
        if (hasOldKey && mCurrentKey == null) {
            mController.onCancelMoreKeysPanel()
        }
    }

    override fun onUpEvent(x: Int, y: Int, pointerId: Int) {
        if (mActivePointerId != pointerId) return
        mCurrentKey = detectKey(x, y)
        mCurrentKey?.let { key ->
            updateReleaseKeyGraphics(key)
            onKeyInput(key)
            mCurrentKey = null
        }
    }

    protected open fun onKeyInput(key: Key) {
        val code = key.code
        if (code == Constants.CODE_OUTPUT_TEXT) {
            mListener?.onTextInput(mCurrentKey!!.outputText)
        } else if (code != Constants.CODE_UNSPECIFIED) {
            mListener?.onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        }
    }

    private fun detectKey(x: Int, y: Int): Key? {
        val oldKey = mCurrentKey
        val newKey = mKeyDetector.detectHitKey(x, y)
        if (newKey === oldKey) return newKey
        oldKey?.let {
            updateReleaseKeyGraphics(it)
            invalidateKey(it)
        }
        newKey?.let {
            updatePressKeyGraphics(it)
            invalidateKey(it)
        }
        return newKey
    }

    private fun updateReleaseKeyGraphics(key: Key) {
        key.onReleased()
        invalidateKey(key)
    }

    private fun updatePressKeyGraphics(key: Key) {
        key.onPressed()
        invalidateKey(key)
    }

    override fun dismissMoreKeysPanel() {
        if (!isShowingInParent()) return
        mController.onDismissMoreKeysPanel()
    }

    override fun translateX(x: Int): Int = x - mOriginX

    override fun translateY(y: Int): Int = y - mOriginY

    override fun onTouchEvent(me: MotionEvent): Boolean {
        val action = me.actionMasked
        val index = me.actionIndex
        val x = me.getX(index).toInt()
        val y = me.getY(index).toInt()
        val pointerId = me.getPointerId(index)
        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(x, y, pointerId)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> onUpEvent(x, y, pointerId)
            MotionEvent.ACTION_MOVE -> onMoveEvent(x, y, pointerId)
        }
        return true
    }

    private val containerView: View
        get() = parent as View

    override fun showInParent(parentView: ViewGroup) {
        removeFromParent()
        parentView.addView(containerView)
    }

    override fun removeFromParent() {
        val containerView = containerView
        val currentParent = containerView.parent as? ViewGroup
        currentParent?.removeView(containerView)
    }

    override fun isShowingInParent(): Boolean = containerView.parent != null
}
