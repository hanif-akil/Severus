package rkr.simplekeyboard.inputmethod.keyboard

import android.util.SparseArray
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyVisualAttributes
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardIconsSet
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import java.util.Collections

class Keyboard(params: KeyboardParams) {
    val mId: KeyboardId? = params.mId
    val mOccupiedHeight: Int = params.mOccupiedHeight
    val mOccupiedWidth: Int = params.mOccupiedWidth
    val mBottomPadding: Float = params.mBottomPadding
    val mVerticalGap: Float = params.mVerticalGap
    val mHorizontalGap: Float = params.mHorizontalGap
    val mKeyVisualAttributes: KeyVisualAttributes? = params.mKeyVisualAttributes
    val mMostCommonKeyHeight: Int = params.mMostCommonKeyHeight
    val mMostCommonKeyWidth: Int = params.mMostCommonKeyWidth
    val mMoreKeysTemplate: Int = params.mMoreKeysTemplate
    private val mSortedKeys: List<Key> = Collections.unmodifiableList(ArrayList(params.mSortedKeys))
    val mShiftKeys: List<Key> = Collections.unmodifiableList(params.mShiftKeys)
    val mAltCodeKeysWhileTyping: List<Key> = Collections.unmodifiableList(params.mAltCodeKeysWhileTyping)
    val mIconsSet: KeyboardIconsSet = params.mIconsSet
    private val mKeyCache = SparseArray<Key?>()
    private val mProximityInfo: ProximityInfo

    init {
        mProximityInfo = ProximityInfo(params.mGridWidth, params.mGridHeight, mOccupiedWidth, mOccupiedHeight, mSortedKeys)
    }

    fun getSortedKeys(): List<Key> = mSortedKeys

    fun getKey(code: Int): Key? {
        if (code == Constants.CODE_UNSPECIFIED) return null
        synchronized(mKeyCache) {
            val index = mKeyCache.indexOfKey(code)
            if (index >= 0) return mKeyCache.valueAt(index)
            for (key in getSortedKeys()) {
                if (key.getCode() == code) {
                    mKeyCache.put(code, key)
                    return key
                }
            }
            mKeyCache.put(code, null)
            return null
        }
    }

    fun hasKey(aKey: Key): Boolean {
        if (mKeyCache.indexOfValue(aKey) >= 0) return true
        for (key in getSortedKeys()) {
            if (key === aKey) {
                mKeyCache.put(key.getCode(), key)
                return true
            }
        }
        return false
    }

    override fun toString(): String = mId.toString()

    fun getNearestKeys(x: Int, y: Int): List<Key> {
        val adjustedX = maxOf(0, minOf(x, mOccupiedWidth - 1))
        val adjustedY = maxOf(0, minOf(y, mOccupiedHeight - 1))
        return mProximityInfo.getNearestKeys(adjustedX, adjustedY)
    }
}
