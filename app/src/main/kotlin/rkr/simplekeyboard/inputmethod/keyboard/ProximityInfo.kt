package rkr.simplekeyboard.inputmethod.keyboard

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
    private val mGridNeighbors: Array<List<Key>>

    init {
        mGridWidth = gridWidth
        mGridHeight = gridHeight
        mGridSize = mGridWidth * mGridHeight
        mCellWidth = (minWidth + mGridWidth - 1) / mGridWidth
        mCellHeight = (height + mGridHeight - 1) / mGridHeight
        mKeyboardMinWidth = minWidth
        mKeyboardHeight = height
        mSortedKeys = sortedKeys
        mGridNeighbors = arrayOfNulls<List<Key>>(mGridSize) as Array<List<Key>>
        if (minWidth != 0 && height != 0) {
            computeNearestNeighbors()
        }
    }

    @Suppress("UNCHECKED_CAST")
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
            val keyTop = keyY - key.topPadding
            val keyBottom = minOf(keyY + key.height + key.bottomPadding, maxKeyBottom)
            val keyLeft = keyX - key.leftPadding
            val keyRight = minOf(keyX + key.width + key.rightPadding, maxKeyRight)
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
            if (index < mGridSize) return mGridNeighbors[index]
        }
        return EMPTY_KEY_LIST
    }

    companion object {
        private val EMPTY_KEY_LIST: List<Key> = emptyList()
    }
}
