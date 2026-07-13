/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.util.ArrayList
import java.util.Collections

class ProximityInfo(
    gridWidth: Int,
    gridHeight: Int,
    minWidth: Int,
    height: Int,
    sortedKeys: List<Key>
) {
    private val mGridWidth: Int
    private val mGridHeight: Int
    private val mGridSize: Int
    private val mCellWidth: Int
    private val mCellHeight: Int
    private val mKeyboardMinWidth: Int
    private val mKeyboardHeight: Int
    private val mSortedKeys: List<Key>
    private val mGridNeighbors: Array<List<Key>?>

    init {
        mGridWidth = gridWidth
        mGridHeight = gridHeight
        mGridSize = mGridWidth * mGridHeight
        mCellWidth = (minWidth + mGridWidth - 1) / mGridWidth
        mCellHeight = (height + mGridHeight - 1) / mGridHeight
        mKeyboardMinWidth = minWidth
        mKeyboardHeight = height
        mSortedKeys = sortedKeys
        @Suppress("UNCHECKED_CAST")
        mGridNeighbors = arrayOfNulls<List<Key>>(mGridSize) as Array<List<Key>?>
        if (minWidth != 0 && height != 0) {
            computeNearestNeighbors()
        }
    }

    private fun computeNearestNeighbors() {
        val keyCount = mSortedKeys.size
        val gridSize = mGridNeighbors.size
        val maxKeyRight = mGridWidth * mCellWidth
        val maxKeyBottom = mGridHeight * mCellHeight

        val neighborsFlatBuffer = arrayOfNulls<Key>(gridSize * keyCount)
        val neighborCountPerCell = IntArray(gridSize)
        for (key in mSortedKeys) {
            if (key.isSpacer) continue

            val keyX = key.x
            val keyY = key.y
            val keyTop = keyY - key.getTopPadding()
            val keyBottom = Math.min(
                keyY + key.height + key.getBottomPadding(),
                maxKeyBottom
            )
            val keyLeft = keyX - key.getLeftPadding()
            val keyRight = Math.min(
                keyX + key.width + key.getRightPadding(),
                maxKeyRight
            )
            val yDeltaToGrid = keyTop % mCellHeight
            val xDeltaToGrid = keyLeft % mCellWidth
            val yStart = keyTop - yDeltaToGrid
            val xStart = keyLeft - xDeltaToGrid
            var baseIndexOfCurrentRow = (yStart / mCellHeight) * mGridWidth + (xStart / mCellWidth)
            var cellTop = yStart
            while (cellTop < keyBottom) {
                var index = baseIndexOfCurrentRow
                var cellLeft = xStart
                while (cellLeft < keyRight) {
                    neighborsFlatBuffer[index * keyCount + neighborCountPerCell[index]] = key
                    neighborCountPerCell[index]++
                    index++
                    cellLeft += mCellWidth
                }
                baseIndexOfCurrentRow += mGridWidth
                cellTop += mCellHeight
            }
        }

        for (i in 0 until gridSize) {
            val indexStart = i * keyCount
            val indexEnd = indexStart + neighborCountPerCell[i]
            val neighbors = ArrayList<Key>(indexEnd - indexStart)
            for (index in indexStart until indexEnd) {
                neighbors.add(neighborsFlatBuffer[index]!!)
            }
            mGridNeighbors[i] = Collections.unmodifiableList(neighbors)
        }
    }

    fun getNearestKeys(x: Int, y: Int): List<Key> {
        if (x in 0 until mKeyboardMinWidth && y in 0 until mKeyboardHeight) {
            val index = (y / mCellHeight) * mGridWidth + (x / mCellWidth)
            if (index < mGridSize) {
                return mGridNeighbors[index] ?: EMPTY_KEY_LIST
            }
        }
        return EMPTY_KEY_LIST
    }

    companion object {
        private val EMPTY_KEY_LIST: List<Key> = emptyList()
    }
}
