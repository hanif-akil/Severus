/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2020 wittmane
 * Copyright (C) 2019 Raimondas Rimkus
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

import android.util.SparseArray
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyVisualAttributes
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardIconsSet
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams
import rkr.simplekeyboard.inputmethod.latin.common.Constants

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 */
open class Keyboard(params: KeyboardParams) {
    val mId: KeyboardId = params.mId

    /** Total height of the keyboard, including the padding and keys */
    val mOccupiedHeight: Int = params.mOccupiedHeight
    /** Total width of the keyboard, including the padding and keys */
    val mOccupiedWidth: Int = params.mOccupiedWidth

    /** The padding below the keyboard */
    val mBottomPadding: Float = params.mBottomPadding
    /** Default gap between rows */
    val mVerticalGap: Float = params.mVerticalGap
    /** Default gap between columns */
    val mHorizontalGap: Float = params.mHorizontalGap

    /** Per keyboard key visual parameters */
    val mKeyVisualAttributes: KeyVisualAttributes = params.mKeyVisualAttributes

    val mMostCommonKeyHeight: Int = params.mMostCommonKeyHeight
    val mMostCommonKeyWidth: Int = params.mMostCommonKeyWidth

    /** More keys keyboard template */
    val mMoreKeysTemplate: Int = params.mMoreKeysTemplate

    /** List of keys in this keyboard */
    private val mSortedKeys: List<Key> = java.util.Collections.unmodifiableList(
        ArrayList(params.mSortedKeys)
    )
    val mShiftKeys: List<Key> = java.util.Collections.unmodifiableList(params.mShiftKeys)
    val mAltCodeKeysWhileTyping: List<Key> = java.util.Collections.unmodifiableList(params.mAltCodeKeysWhileTyping)
    val mIconsSet: KeyboardIconsSet = params.mIconsSet

    private val mKeyCache = SparseArray<Key?>()

    private val mProximityInfo: ProximityInfo

    init {
        mProximityInfo = ProximityInfo(
            params.mGridWidth, params.mGridHeight,
            mOccupiedWidth, mOccupiedHeight, mSortedKeys
        )
    }

    /**
     * Return the sorted list of keys of this keyboard.
     * The keys are sorted from top-left to bottom-right order.
     * The list may contain [Key.Spacer] object as well.
     * @return the sorted unmodifiable list of [Key]s of this keyboard.
     */
    fun getSortedKeys(): List<Key> = mSortedKeys

    fun getKey(code: Int): Key? {
        if (code == Constants.CODE_UNSPECIFIED) {
            return null
        }
        synchronized(mKeyCache) {
            val index = mKeyCache.indexOfKey(code)
            if (index >= 0) {
                return mKeyCache.valueAt(index)
            }

            for (key in getSortedKeys()) {
                if (key.code == code) {
                    mKeyCache.put(code, key)
                    return key
                }
            }
            mKeyCache.put(code, null)
            return null
        }
    }

    fun hasKey(aKey: Key): Boolean {
        if (mKeyCache.indexOfValue(aKey) >= 0) {
            return true
        }

        for (key in getSortedKeys()) {
            if (key === aKey) {
                mKeyCache.put(key.code, key)
                return true
            }
        }
        return false
    }

    override fun toString(): String = mId.toString()

    /**
     * Returns the array of the keys that are closest to the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the list of the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    fun getNearestKeys(x: Int, y: Int): List<Key> {
        // Avoid dead pixels at edges of the keyboard
        val adjustedX = Math.max(0, Math.min(x, mOccupiedWidth - 1))
        val adjustedY = Math.max(0, Math.min(y, mOccupiedHeight - 1))
        return mProximityInfo.getNearestKeys(adjustedX, adjustedY)
    }
}
