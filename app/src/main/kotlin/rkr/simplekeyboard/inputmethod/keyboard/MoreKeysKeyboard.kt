/*
 * Copyright (C) 2011 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.keyboard

import android.content.Context
import android.graphics.Paint
import android.util.Log

import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardBuilder
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams
import rkr.simplekeyboard.inputmethod.keyboard.internal.MoreKeySpec
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils
import rkr.simplekeyboard.inputmethod.latin.utils.TypefaceUtils

class MoreKeysKeyboard(params: MoreKeysKeyboardParams) : Keyboard(params) {
    private val mDefaultKeyCoordX: Int

    init {
        mDefaultKeyCoordX = Math.round(
            params.getDefaultKeyCoordX() + params.mOffsetX
                    + (params.mDefaultKeyPaddedWidth - params.mHorizontalGap) / 2
        )
    }

    fun getDefaultCoordX(): Int = mDefaultKeyCoordX

    class MoreKeysKeyboardParams : KeyboardParams() {
        var mIsMoreKeysFixedOrder: Boolean = false
        var mTopRowAdjustment: Int = 0
        var mNumRows: Int = 0
        var mNumColumns: Int = 0
        var mTopKeys: Int = 0
        var mLeftKeys: Int = 0
        var mRightKeys: Int = 0 // includes default key.
        var mColumnWidth: Float = 0f
        var mOffsetX: Float = 0f

        fun setParameters(
            numKeys: Int, numColumn: Int,
            keyPaddedWidth: Float, rowHeight: Float,
            coordXInParent: Float, parentKeyboardWidth: Int,
            isMoreKeysFixedColumn: Boolean,
            isMoreKeysFixedOrder: Boolean
        ) {
            val availableWidth = parentKeyboardWidth - mLeftPadding - mRightPadding + mHorizontalGap
            if (availableWidth < keyPaddedWidth) {
                throw IllegalArgumentException(
                    "Keyboard is too small to hold more keys: $availableWidth $keyPaddedWidth"
                )
            }
            mIsMoreKeysFixedOrder = isMoreKeysFixedOrder
            mDefaultKeyPaddedWidth = keyPaddedWidth
            mDefaultRowHeight = rowHeight

            val maxColumns = getMaxKeys(availableWidth, keyPaddedWidth)
            if (isMoreKeysFixedColumn) {
                val requestedNumColumns = minOf(numKeys, numColumn)
                if (maxColumns < requestedNumColumns) {
                    Log.e(TAG, "Keyboard is too small to hold the requested more keys columns: " +
                            "$availableWidth $keyPaddedWidth $numKeys $requestedNumColumns. The number of columns was reduced.")
                    mNumColumns = maxColumns
                } else {
                    mNumColumns = requestedNumColumns
                }
                mNumRows = getNumRows(numKeys, mNumColumns)
            } else {
                val defaultNumColumns = minOf(maxColumns, numColumn)
                mNumRows = getNumRows(numKeys, defaultNumColumns)
                mNumColumns = getOptimizedColumns(numKeys, defaultNumColumns, mNumRows)
            }
            val topKeys = numKeys % mNumColumns
            mTopKeys = if (topKeys == 0) mNumColumns else topKeys

            val numLeftKeys = (mNumColumns - 1) / 2
            val numRightKeys = mNumColumns - numLeftKeys
            val leftWidth = maxOf(
                coordXInParent - mLeftPadding - keyPaddedWidth / 2 + mHorizontalGap / 2,
                0f
            )
            val rightWidth = maxOf(
                parentKeyboardWidth - coordXInParent + keyPaddedWidth / 2 - mRightPadding + mHorizontalGap / 2,
                0f
            )
            var maxLeftKeys = getMaxKeys(leftWidth, keyPaddedWidth)
            var maxRightKeys = getMaxKeys(rightWidth, keyPaddedWidth)
            if (numKeys >= mNumColumns && mNumColumns == maxColumns
                && maxLeftKeys + maxRightKeys < maxColumns
            ) {
                val extraLeft = leftWidth - maxLeftKeys * keyPaddedWidth
                val extraRight = rightWidth - maxRightKeys * keyPaddedWidth
                if (extraLeft > extraRight) {
                    maxLeftKeys++
                } else {
                    maxRightKeys++
                }
            }

            val leftKeys: Int
            val rightKeys: Int
            if (numLeftKeys > maxLeftKeys) {
                leftKeys = maxLeftKeys
                rightKeys = mNumColumns - leftKeys
            } else if (numRightKeys > maxRightKeys) {
                rightKeys = maxOf(maxRightKeys, 1)
                leftKeys = mNumColumns - rightKeys
            } else {
                leftKeys = numLeftKeys
                rightKeys = numRightKeys
            }
            mLeftKeys = leftKeys
            mRightKeys = rightKeys

            mTopRowAdjustment = getTopRowAdjustment()
            mColumnWidth = mDefaultKeyPaddedWidth
            mBaseWidth = mNumColumns * mColumnWidth
            mOccupiedWidth = Math.round(mBaseWidth + mLeftPadding + mRightPadding - mHorizontalGap)
            mBaseHeight = mNumRows * mDefaultRowHeight
            mOccupiedHeight = Math.round(mBaseHeight + mTopPadding + mBottomPadding - mVerticalGap)

            mGridWidth = minOf(mGridWidth, mNumColumns)
            mGridHeight = minOf(mGridHeight, mNumRows)
        }

        private fun getTopRowAdjustment(): Int {
            val numOffCenterKeys = Math.abs(mRightKeys - 1 - mLeftKeys)
            if (mTopKeys > mNumColumns - numOffCenterKeys || mTopKeys % 2 == 1) {
                return 0
            }
            return -1
        }

        fun getColumnPos(n: Int): Int {
            return if (mIsMoreKeysFixedOrder) getFixedOrderColumnPos(n) else getAutomaticColumnPos(n)
        }

        private fun getFixedOrderColumnPos(n: Int): Int {
            val col = n % mNumColumns
            val row = n / mNumColumns
            if (!isTopRow(row)) {
                return col - mLeftKeys
            }
            val rightSideKeys = mTopKeys / 2
            val leftSideKeys = mTopKeys - (rightSideKeys + 1)
            val pos = col - leftSideKeys
            val numLeftKeys = mLeftKeys + mTopRowAdjustment
            val numRightKeys = mRightKeys - 1
            return when {
                numRightKeys >= rightSideKeys && numLeftKeys >= leftSideKeys -> pos
                numRightKeys < rightSideKeys -> pos - (rightSideKeys - numRightKeys)
                else -> pos + (leftSideKeys - numLeftKeys)
            }
        }

        private fun getAutomaticColumnPos(n: Int): Int {
            val col = n % mNumColumns
            val row = n / mNumColumns
            var leftKeys = mLeftKeys
            if (isTopRow(row)) {
                leftKeys += mTopRowAdjustment
            }
            if (col == 0) {
                return 0
            }

            var pos = 0
            var right = 1
            var left = 0
            var i = 0
            while (true) {
                if (right < mRightKeys) {
                    pos = right
                    right++
                    i++
                }
                if (i >= col) break
                if (left < leftKeys) {
                    left++
                    pos = -left
                    i++
                }
                if (i >= col) break
            }
            return pos
        }

        fun getDefaultKeyCoordX(): Float {
            return mLeftKeys * mColumnWidth + mLeftPadding
        }

        fun getX(n: Int, row: Int): Float {
            val x = getColumnPos(n) * mColumnWidth + getDefaultKeyCoordX()
            return if (isTopRow(row)) {
                x + mTopRowAdjustment * (mColumnWidth / 2)
            } else {
                x
            }
        }

        fun getY(row: Int): Float {
            return (mNumRows - 1 - row) * mDefaultRowHeight + mTopPadding
        }

        private fun isTopRow(rowCount: Int): Boolean {
            return mNumRows > 1 && rowCount == mNumRows - 1
        }

        companion object {
            private const val FLOAT_THRESHOLD = 0.0001f

            private fun getTopRowEmptySlots(numKeys: Int, numColumns: Int): Int {
                val remainings = numKeys % numColumns
                return if (remainings == 0) 0 else numColumns - remainings
            }

            private fun getOptimizedColumns(numKeys: Int, maxColumns: Int, numRows: Int): Int {
                var numColumns = minOf(numKeys, maxColumns)
                while (getTopRowEmptySlots(numKeys, numColumns) >= numRows) {
                    numColumns--
                }
                return numColumns
            }

            private fun getNumRows(numKeys: Int, numColumn: Int): Int {
                return (numKeys + numColumn - 1) / numColumn
            }

            private fun getMaxKeys(keyboardWidth: Float, keyPaddedWidth: Float): Int {
                val maxKeys = Math.round(keyboardWidth / keyPaddedWidth)
                return if (maxKeys * keyPaddedWidth > keyboardWidth + FLOAT_THRESHOLD) {
                    maxKeys - 1
                } else {
                    maxKeys
                }
            }
        }
    }

    class Builder(
        context: Context,
        key: Key,
        keyboard: Keyboard,
        isSingleMoreKeyWithPreview: Boolean,
        keyPreviewVisibleWidth: Int,
        keyPreviewVisibleHeight: Int,
        paintToMeasure: Paint
    ) : KeyboardBuilder<MoreKeysKeyboardParams>(context, MoreKeysKeyboardParams()) {
        private val mParentKey: Key

        init {
            load(keyboard.mMoreKeysTemplate, keyboard.mId)

            mParams.mVerticalGap = keyboard.mVerticalGap / 2
            mParentKey = key

            val keyPaddedWidth: Float
            val rowHeight: Float
            if (isSingleMoreKeyWithPreview) {
                val keyboardHorizontalPadding = mParams.mLeftPadding + mParams.mRightPadding
                val baseKeyPaddedWidth = keyPreviewVisibleWidth + mParams.mHorizontalGap
                keyPaddedWidth = if (keyboardHorizontalPadding > baseKeyPaddedWidth - FLOAT_THRESHOLD) {
                    baseKeyPaddedWidth
                } else {
                    mParams.mOffsetX = (mParams.mRightPadding - mParams.mLeftPadding) / 2
                    baseKeyPaddedWidth - keyboardHorizontalPadding
                }
                val baseKeyPaddedHeight = keyPreviewVisibleHeight + mParams.mVerticalGap
                rowHeight = if (mParams.mTopPadding > baseKeyPaddedHeight - FLOAT_THRESHOLD) {
                    baseKeyPaddedHeight
                } else {
                    baseKeyPaddedHeight - mParams.mTopPadding
                }
            } else {
                val defaultKeyWidth = mParams.mDefaultKeyPaddedWidth - mParams.mHorizontalGap
                val padding = context.resources.getDimension(
                    R.dimen.config_more_keys_keyboard_key_horizontal_padding
                ) + if (key.hasLabelsInMoreKeys())
                    defaultKeyWidth * LABEL_PADDING_RATIO else 0.0f
                keyPaddedWidth = getMaxKeyWidth(key, defaultKeyWidth, padding, paintToMeasure) +
                        mParams.mHorizontalGap
                rowHeight = keyboard.mMostCommonKeyHeight + keyboard.mVerticalGap
            }
            val moreKeys = key.getMoreKeys()!!
            mParams.setParameters(
                moreKeys.size, key.getMoreKeysColumnNumber(), keyPaddedWidth,
                rowHeight, key.getX() + key.getWidth() / 2f, keyboard.mId.mWidth,
                key.isMoreKeysFixedColumn(), key.isMoreKeysFixedOrder()
            )
        }

        override fun build(): MoreKeysKeyboard {
            val params = mParams
            val moreKeyFlags = mParentKey.getMoreKeyLabelFlags()
            val moreKeys = mParentKey.getMoreKeys()!!
            for (n in moreKeys.indices) {
                val moreKeySpec = moreKeys[n]
                val row = n / params.mNumColumns
                val width = params.mDefaultKeyPaddedWidth - params.mHorizontalGap
                val height = params.mDefaultRowHeight - params.mVerticalGap
                val keyLeftEdge = params.getX(n, row)
                val keyTopEdge = params.getY(row)
                val keyRightEdge = keyLeftEdge + width
                val keyBottomEdge = keyTopEdge + height

                val keyboardLeftEdge = params.mLeftPadding
                val keyboardRightEdge = params.mOccupiedWidth - params.mRightPadding
                val keyboardTopEdge = params.mTopPadding
                val keyboardBottomEdge = params.mOccupiedHeight - params.mBottomPadding

                val keyLeftPadding = if (keyLeftEdge < keyboardLeftEdge + FLOAT_THRESHOLD)
                    params.mLeftPadding else params.mHorizontalGap / 2
                val keyRightPadding = if (keyRightEdge > keyboardRightEdge - FLOAT_THRESHOLD)
                    params.mRightPadding else params.mHorizontalGap / 2
                val keyTopPadding = if (keyTopEdge < keyboardTopEdge + FLOAT_THRESHOLD)
                    params.mTopPadding else params.mVerticalGap / 2
                val keyBottomPadding = if (keyBottomEdge > keyboardBottomEdge - FLOAT_THRESHOLD)
                    params.mBottomPadding else params.mVerticalGap / 2

                val key = moreKeySpec.buildKey(
                    keyLeftEdge, keyTopEdge, width, height,
                    keyLeftPadding, keyRightPadding, keyTopPadding, keyBottomPadding,
                    moreKeyFlags
                )
                params.onAddKey(key)
            }
            return MoreKeysKeyboard(params)
        }

        companion object {
            private const val LABEL_PADDING_RATIO = 0.2f
            private const val FLOAT_THRESHOLD = 0.0001f

            private fun getMaxKeyWidth(parentKey: Key, minKeyWidth: Float, padding: Float, paint: Paint): Float {
                var maxWidth = minKeyWidth
                for (spec in parentKey.getMoreKeys()!!) {
                    val label = spec.mLabel
                    if (label != null && StringUtils.codePointCount(label) > 1) {
                        maxWidth = maxOf(
                            maxWidth,
                            TypefaceUtils.getStringWidth(label, paint) + padding
                        )
                    }
                }
                return maxWidth
            }
        }
    }

    companion object {
        private const val TAG = "MoreKeysKeyboard"
    }
}
