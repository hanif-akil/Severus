package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker

class TimerHandler(looper: Looper, private val mTimerProxy: TimerProxy) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_KEY_REPEAT -> {
                val tracker = msg.obj as PointerTracker
                tracker.KeyRepeat()
            }
            MSG_LONGPRESS -> {
                val tracker = msg.obj as PointerTracker
                tracker.LongPressed()
            }
            MSG_LONGPRESS_SHIFT -> {
                mTimerProxy.cancelLongPressShiftKeyTimer()
            }
            MSG_DOUBLE_TAP_SHIFT_KEY -> {
                mTimerProxy.cancelDoubleTapShiftKeyTimer()
            }
            MSG_TYPING_STATE -> {
                // Just clear the typing state timer.
            }
        }
    }

    fun startKeyRepeatTimer(tracker: PointerTracker, repeatCount: Int, delay: Int) {
        removeMessages(MSG_KEY_REPEAT)
        val msg = obtainMessage(MSG_KEY_REPEAT, repeatCount, 0, tracker)
        sendMessageDelayed(msg, delay.toLong())
    }

    fun startLongPressTimer(tracker: PointerTracker, delay: Int) {
        removeMessages(MSG_LONGPRESS)
        val msg = obtainMessage(MSG_LONGPRESS, tracker)
        sendMessageDelayed(msg, delay.toLong())
    }

    fun cancelLongPressTimer() {
        removeMessages(MSG_LONGPRESS)
    }

    fun cancelLongPressShiftKeyTimer() {
        removeMessages(MSG_LONGPRESS_SHIFT)
    }

    fun cancelKeyTimers() {
        removeMessages(MSG_KEY_REPEAT)
        removeMessages(MSG_LONGPRESS)
    }

    fun startDoubleTapShiftKeyTimer() {
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
        val msg = obtainMessage(MSG_DOUBLE_TAP_SHIFT_KEY)
        sendMessageDelayed(msg, DOUBLE_TAP_SHIFT_KEY_DELAY.toLong())
    }

    fun cancelDoubleTapShiftKeyTimer() {
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
    }

    fun isInDoubleTapShiftKeyTimeout(): Boolean {
        return hasMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
    }

    companion object {
        private const val MSG_KEY_REPEAT = 0
        private const val MSG_LONGPRESS = 1
        private const val MSG_LONGPRESS_SHIFT = 2
        private const val MSG_DOUBLE_TAP_SHIFT_KEY = 3
        private const val MSG_TYPING_STATE = 4
        private const val DOUBLE_TAP_SHIFT_KEY_DELAY = 500
    }
}
