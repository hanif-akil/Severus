/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2021 Raimondas Rimkus
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

import android.util.SparseIntArray
import java.util.TreeSet
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId
import rkr.simplekeyboard.inputmethod.latin.common.Constants

open class KeyboardParams {
    var mId: KeyboardId? = null
    var mOccupiedHeight = 0
    var mOccupiedWidth = 0
    var mBaseHeight = 0f
    var mBaseWidth = 0f
    var mTopPadding = 0f
    var mBottomPadding = 0f
    var mLeftPadding = 0f
    var mRightPadding = 0f
    var mKeyVisualAttributes: KeyVisualAttributes? = null
    var mDefaultRowHeight = 0f
    var mDefaultKeyPaddedWidth = 0f
    var mHorizontalGap = 0f
    var mVerticalGap = 0f
    var mMoreKeysTemplate = 0
    var mMaxMoreKeysKeyboardColumn = 0
    var mGridWidth = 0
    var mGridHeight = 0
    val mSortedKeys: MutableSet<Key> = TreeSet(ROW_COLUMN_COMPARATOR)
    val mShiftKeys = ArrayList<Key>()
    val mAltCodeKeysWhileTyping = ArrayList<Key>()
    val mIconsSet = KeyboardIconsSet()
    val mTextsSet = KeyboardTextsSet()
    val mKeyStyles = KeyStylesSet(mTextsSet)
    private val mUniqueKeysCache: UniqueKeysCache
    var mAllowRedundantMoreKeys = false
    var mMostCommonKeyHeight = 0
    var mMostCommonKeyWidth = 0
    private var mMaxHeightCount = 0
    private var mMaxWidthCount = 0
    private val mHeightHistogram = SparseIntArray()
    private val mWidthHistogram = SparseIntArray()

    constructor() : this(UniqueKeysCache.NO_CACHE)

    constructor(keysCache: UniqueKeysCache) {
        mUniqueKeysCache = keysCache
    }

    fun onAddKey(newKey: Key) {
        val key = mUniqueKeysCache.getUniqueKey(newKey)
        val isSpacer = key.isSpacer
        if (isSpacer && key.width == 0f) return
        mSortedKeys.add(key)
        if (isSpacer) return
        updateHistogram(key)
        if (key.code == Constants.CODE_SHIFT) mShiftKeys.add(key)
        if (key.altCodeWhileTyping()) mAltCodeKeysWhileTyping.add(key)
    }

    fun removeRedundantMoreKeys() {
        if (mAllowRedundantMoreKeys) return
        val lettersOnBaseLayout = MoreKeySpec.LettersOnBaseLayout()
        for (key in mSortedKeys) {
            lettersOnBaseLayout.addLetter(key)
        }
        val allKeys = ArrayList(mSortedKeys)
        mSortedKeys.clear()
        for (key in allKeys) {
            val filteredKey = Key.removeRedundantMoreKeys(key, lettersOnBaseLayout)
            mSortedKeys.add(mUniqueKeysCache.getUniqueKey(filteredKey))
        }
    }

    private fun updateHistogram(key: Key) {
        val height = Math.round(key.definedHeight)
        val heightCount = updateHistogramCounter(mHeightHistogram, height)
        if (heightCount > mMaxHeightCount) {
            mMaxHeightCount = heightCount
            mMostCommonKeyHeight = height
        }
        val width = Math.round(key.definedWidth)
        val widthCount = updateHistogramCounter(mWidthHistogram, width)
        if (widthCount > mMaxWidthCount) {
            mMaxWidthCount = widthCount
            mMostCommonKeyWidth = width
        }
    }

    companion object {
        private val ROW_COLUMN_COMPARATOR = Comparator<Key> { lhs, rhs ->
            if (lhs.y < rhs.y) -1
            else if (lhs.y > rhs.y) 1
            else if (lhs.x < rhs.x) -1
            else if (lhs.x > rhs.x) 1
            else 0
        }

        private fun updateHistogramCounter(histogram: SparseIntArray, key: Int): Int {
            val index = histogram.indexOfKey(key)
            val count = (if (index >= 0) histogram.get(key) else 0) + 1
            histogram.put(key, count)
            return count
        }
    }
}
