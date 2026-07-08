package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.view.MotionEvent

class BogusMoveEventDetector {
    private var mLastDownEventTime: Long = 0
    private var mLastMoveEventTime: Long = 0

    fun onDownEvent(eventTime: Long) {
        mLastDownEventTime = eventTime
    }

    fun onMoveEvent(eventTime: Long): Boolean {
        if (eventTime - mLastDownEventTime < MAX_TIME_TO_DETECT_BOGUS_MOVE_EVENT) {
            if (mLastMoveEventTime != 0L && eventTime - mLastMoveEventTime > MIN_TIME_INTERVAL_TO_CONSIDER_BOUGUS_MOVE_EVENT) {
                mLastMoveEventTime = eventTime
                return false
            }
            mLastMoveEventTime = eventTime
            return true
        }
        return false
    }

    fun onUpEvent() {
        mLastMoveEventTime = 0
    }

    companion object {
        private const val MAX_TIME_TO_DETECT_BOGUS_MOVE_EVENT = 50L
        private const val MIN_TIME_INTERVAL_TO_CONSIDER_BOUGUS_MOVE_EVENT = 10L
    }
}
