package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.util.Log
import android.view.MotionEvent
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.KeyDetector
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils

class NonDistinctMultitouchHelper {
    private var mOldPointerCount = 1
    private var mOldKey: Key? = null
    private val mLastCoords = CoordinateUtils.newInstance()

    fun processMotionEvent(me: MotionEvent, keyDetector: KeyDetector) {
        val pointerCount = me.pointerCount
        val oldPointerCount = mOldPointerCount
        mOldPointerCount = pointerCount
        if (pointerCount > 1 && oldPointerCount > 1) return

        val mainTracker = PointerTracker.getPointerTracker(MAIN_POINTER_TRACKER_ID)
        val action = me.actionMasked
        val index = me.actionIndex
        val eventTime = me.eventTime
        val downTime = me.downTime

        if (oldPointerCount == 1 && pointerCount == 1) {
            if (me.getPointerId(index) == mainTracker.mPointerId) {
                mainTracker.processMotionEvent(me, keyDetector)
                return
            }
            injectMotionEvent(action, me.getX(index), me.getY(index), downTime, eventTime, mainTracker, keyDetector)
            return
        }

        if (oldPointerCount == 1 && pointerCount == 2) {
            mainTracker.getLastCoordinates(mLastCoords)
            val x = CoordinateUtils.x(mLastCoords)
            val y = CoordinateUtils.y(mLastCoords)
            mOldKey = mainTracker.getKeyOn(x, y)
            injectMotionEvent(MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), downTime, eventTime, mainTracker, keyDetector)
            return
        }

        if (oldPointerCount == 2 && pointerCount == 1) {
            val x = me.getX(index).toInt()
            val y = me.getY(index).toInt()
            val newKey = mainTracker.getKeyOn(x, y)
            if (mOldKey != newKey) {
                injectMotionEvent(MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat(), downTime, eventTime, mainTracker, keyDetector)
                if (action == MotionEvent.ACTION_UP) {
                    injectMotionEvent(MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), downTime, eventTime, mainTracker, keyDetector)
                }
            }
            return
        }

        Log.w(TAG, "Unknown touch panel behavior: pointer count is $pointerCount (previously $oldPointerCount)")
    }

    companion object {
        private const val TAG = "NonDistinctMultitouchHelper"
        private const val MAIN_POINTER_TRACKER_ID = 0

        private fun injectMotionEvent(
            action: Int, x: Float, y: Float, downTime: Long, eventTime: Long,
            tracker: PointerTracker, keyDetector: KeyDetector
        ) {
            val me = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
            try {
                tracker.processMotionEvent(me, keyDetector)
            } finally {
                me.recycle()
            }
        }
    }
}
