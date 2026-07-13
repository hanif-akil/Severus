/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2021 Raimondas Rimkus
 * Copyright (C) 2020 wittmane
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
import android.content.res.TypedArray
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.ArrayDeque
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.Keyboard
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils

class KeyboardRow(
    private val res: Resources,
    private val mParams: KeyboardParams,
    parser: XmlPullParser,
    y: Float
) {
    private val mY: Float
    private val mRowHeight: Float
    private val mKeyTopPadding: Float
    private val mKeyBottomPadding: Float
    private var mNextKeyXPos: Float
    private var mCurrentX = 0f
    private var mCurrentKeyWidth = 0f
    private var mCurrentKeyLeftPadding = 0f
    private var mCurrentKeyRightPadding = 0f
    private var mLastKeyWasSpacer = false
    private var mLastKeyRightEdge = 0f
    private val mRowAttributesStack = ArrayDeque<RowAttributes>()

    init {
        val keyboardAttr = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard)
        mKeyTopPadding = if (y < FLOAT_THRESHOLD) {
            mParams.mTopPadding
        } else {
            0f
        }
        val baseRowHeight = ResourceUtils.getDimensionOrFraction(keyboardAttr, R.styleable.Keyboard_rowHeight, mParams.mBaseHeight, mParams.mDefaultRowHeight)
        var keyHeight = baseRowHeight - mParams.mVerticalGap
        val rowEndY = y + mKeyTopPadding + keyHeight + mParams.mVerticalGap
        val keyboardBottomEdge = mParams.mOccupiedHeight - mParams.mBottomPadding
        if (rowEndY > keyboardBottomEdge - FLOAT_THRESHOLD) {
            val keyEndY = y + mKeyTopPadding + keyHeight
            val keyOverflow = keyEndY - keyboardBottomEdge
            if (keyOverflow > FLOAT_THRESHOLD) {
                if (Math.round(keyOverflow) > 0) {
                    Log.e(TAG, "The row is too tall to fit in the keyboard ($keyOverflow px). The height was reduced to fit.")
                }
                keyHeight = Math.max(keyboardBottomEdge - y - mKeyTopPadding, 0f)
            }
            mKeyBottomPadding = Math.max(mParams.mOccupiedHeight - keyEndY, 0f)
        } else {
            mKeyBottomPadding = mParams.mVerticalGap
        }
        mRowHeight = mKeyTopPadding + keyHeight + mKeyBottomPadding
        keyboardAttr.recycle()
        val keyAttr = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
        mRowAttributesStack.push(RowAttributes(keyAttr, mParams.mDefaultKeyPaddedWidth, mParams.mBaseWidth))
        keyAttr.recycle()
        mY = y + mKeyTopPadding
        mLastKeyRightEdge = 0f
        mNextKeyXPos = mParams.mLeftPadding
    }

    fun pushRowAttributes(keyAttr: TypedArray) {
        val newAttributes = RowAttributes(keyAttr, mRowAttributesStack.peek(), mParams.mBaseWidth)
        mRowAttributesStack.push(newAttributes)
    }

    fun popRowAttributes() {
        mRowAttributesStack.pop()
    }

    private fun getDefaultKeyPaddedWidth(): Float = mRowAttributesStack.peek().mDefaultKeyPaddedWidth

    fun getDefaultKeyLabelFlags(): Int = mRowAttributesStack.peek().mDefaultKeyLabelFlags

    fun getDefaultBackgroundType(): Int = mRowAttributesStack.peek().mDefaultBackgroundType

    fun updateXPos(keyAttr: TypedArray?) {
        if (keyAttr == null || !keyAttr.hasValue(R.styleable.Keyboard_Key_keyXPos)) return
        val keyXPos = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyXPos, mParams.mBaseWidth, 0f) + mParams.mLeftPadding
        if (keyXPos + FLOAT_THRESHOLD < mLastKeyRightEdge) {
            Log.e(TAG, "The specified keyXPos ($keyXPos) is smaller than the next available x position ($mLastKeyRightEdge). The x position was increased to avoid overlapping keys.")
            mNextKeyXPos = mLastKeyRightEdge
        } else {
            mNextKeyXPos = keyXPos
        }
    }

    fun setCurrentKey(keyAttr: TypedArray?, isSpacer: Boolean) {
        val defaultGap = mParams.mHorizontalGap / 2
        updateXPos(keyAttr)
        val keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding
        var keyWidth: Float
        if (isSpacer) {
            val leftGap = Math.min(mNextKeyXPos - mLastKeyRightEdge - defaultGap, defaultGap)
            mCurrentX = mNextKeyXPos - leftGap
            keyWidth = getKeyWidth(keyAttr) + leftGap
            if (mCurrentX + keyWidth + FLOAT_THRESHOLD < keyboardRightEdge) {
                keyWidth += defaultGap
            }
            mCurrentKeyLeftPadding = 0f
            mCurrentKeyRightPadding = 0f
        } else {
            mCurrentX = mNextKeyXPos
            if (mLastKeyRightEdge < FLOAT_THRESHOLD || mLastKeyWasSpacer) {
                mCurrentKeyLeftPadding = mCurrentX - mLastKeyRightEdge
            } else {
                mCurrentKeyLeftPadding = (mCurrentX - mLastKeyRightEdge) / 2
            }
            keyWidth = getKeyWidth(keyAttr)
            mCurrentKeyRightPadding = defaultGap
        }
        val keyOverflow = mCurrentX + keyWidth - keyboardRightEdge
        if (keyOverflow > FLOAT_THRESHOLD) {
            if (Math.round(keyOverflow) > 0) {
                Log.e(TAG, "The ${if (isSpacer) "spacer" else "key"} is too wide to fit in the keyboard ($keyOverflow px). The width was reduced to fit.")
            }
            keyWidth = Math.max(keyboardRightEdge - mCurrentX, 0f)
        }
        mCurrentKeyWidth = keyWidth
        mLastKeyRightEdge = mCurrentX + keyWidth
        mLastKeyWasSpacer = isSpacer
        mNextKeyXPos = mLastKeyRightEdge + if (isSpacer) defaultGap else mParams.mHorizontalGap
    }

    private fun getKeyWidth(keyAttr: TypedArray?): Float {
        if (keyAttr == null) return getDefaultKeyPaddedWidth() - mParams.mHorizontalGap
        val widthType = ResourceUtils.getEnumValue(keyAttr, R.styleable.Keyboard_Key_keyWidth, KEYWIDTH_NOT_ENUM)
        return when (widthType) {
            KEYWIDTH_FILL_RIGHT -> {
                val keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding
                keyboardRightEdge - mCurrentX
            }
            else -> ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyWidth, mParams.mBaseWidth, getDefaultKeyPaddedWidth()) - mParams.mHorizontalGap
        }
    }

    fun getRowHeight(): Float = mRowHeight
    fun getKeyY(): Float = mY
    fun getKeyX(): Float = mCurrentX
    fun getKeyWidth(): Float = mCurrentKeyWidth
    fun getKeyHeight(): Float = mRowHeight - mKeyTopPadding - mKeyBottomPadding
    fun getKeyTopPadding(): Float = mKeyTopPadding
    fun getKeyBottomPadding(): Float = mKeyBottomPadding
    fun getKeyLeftPadding(): Float = mCurrentKeyLeftPadding
    fun getKeyRightPadding(): Float = mCurrentKeyRightPadding

    private class RowAttributes {
        val mDefaultKeyPaddedWidth: Float
        val mDefaultKeyLabelFlags: Int
        val mDefaultBackgroundType: Int

        constructor(keyAttr: TypedArray, defaultKeyPaddedWidth: Float, keyboardWidth: Float) {
            mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyWidth, keyboardWidth, defaultKeyPaddedWidth)
            mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
            mDefaultBackgroundType = keyAttr.getInt(R.styleable.Keyboard_Key_backgroundType, Key.BACKGROUND_TYPE_NORMAL)
        }

        constructor(keyAttr: TypedArray, defaultRowAttr: RowAttributes, keyboardWidth: Float) {
            mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyWidth, keyboardWidth, defaultRowAttr.mDefaultKeyPaddedWidth)
            mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0) or defaultRowAttr.mDefaultKeyLabelFlags
            mDefaultBackgroundType = keyAttr.getInt(R.styleable.Keyboard_Key_backgroundType, defaultRowAttr.mDefaultBackgroundType)
        }
    }

    companion object {
        private val TAG = KeyboardRow::class.java.simpleName
        private const val FLOAT_THRESHOLD = 0.0001f
        private const val KEYWIDTH_NOT_ENUM = 0
        private const val KEYWIDTH_FILL_RIGHT = -1
    }
}
