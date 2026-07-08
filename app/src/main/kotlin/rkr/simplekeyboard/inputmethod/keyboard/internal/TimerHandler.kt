package rkr.simplekeyboard.inputmethod.keyboard.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker

class TimerHandler(view: View, private val mKeyRepeatTimeout: Int) : Handler(view.context.mainLooper), TimerProxy {
    private var mInKeyRepeat = false
    private var mIsTypingState = false

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_KEY_REPEAT -> {
                val data = msg.obj as KeyRepeatData
                mInKeyRepeat = true
                data.tracker.onKeyRepeat(data.code, data.repeatCount)
            }
            MSG_LONGPRESS -> {
                val tracker = msg.obj as PointerTracker
                tracker.onLongPressed()
            }
            MSG_LONGPRESS_SHIFT -> {
                cancelLongPressShiftKeyTimer()
            }
            MSG_DOUBLE_TAP_SHIFT_KEY -> {
                cancelDoubleTapShiftKeyTimer()
            }
            MSG_TYPING_STATE -> {
                mIsTypingState = false
            }
        }
    }

    override fun startTypingStateTimer(typedKey: Key) {
        mIsTypingState = true
        removeMessages(MSG_TYPING_STATE)
        sendEmptyMessageDelayed(MSG_TYPING_STATE, TYPING_STATE_TIMEOUT.toLong())
    }

    override fun isTypingState(): Boolean = mIsTypingState

    override fun startKeyRepeatTimerOf(tracker: PointerTracker, repeatCount: Int, delay: Int) {
        startKeyRepeatTimer(tracker, repeatCount, delay)
    }

    override fun startLongPressTimerOf(tracker: PointerTracker, delay: Int) {
        startLongPressTimer(tracker, delay)
    }

    override fun cancelLongPressTimersOf(tracker: PointerTracker) {
        cancelLongPressTimer()
    }

    fun startKeyRepeatTimer(tracker: PointerTracker, repeatCount: Int, delay: Int) {
        removeMessages(MSG_KEY_REPEAT)
        val msg = obtainMessage(MSG_KEY_REPEAT, KeyRepeatData(tracker, repeatCount))
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

    fun cancelLongPressTimers() {
        removeMessages(MSG_LONGPRESS)
    }

    override fun cancelLongPressShiftKeyTimer() {
        removeMessages(MSG_LONGPRESS_SHIFT)
    }

    fun cancelKeyTimers() {
        removeMessages(MSG_KEY_REPEAT)
        removeMessages(MSG_LONGPRESS)
    }

    override fun cancelKeyTimersOf(tracker: PointerTracker) {
        cancelKeyTimers()
    }

    override fun startDoubleTapShiftKeyTimer() {
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
        val msg = obtainMessage(MSG_DOUBLE_TAP_SHIFT_KEY)
        sendMessageDelayed(msg, DOUBLE_TAP_SHIFT_KEY_DELAY.toLong())
    }

    override fun cancelDoubleTapShiftKeyTimer() {
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
    }

    override fun isInDoubleTapShiftKeyTimeout(): Boolean {
        return hasMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
    }

    fun isInKeyRepeat(): Boolean = mInKeyRepeat

    fun cancelKeyRepeatTimers() {
        removeMessages(MSG_KEY_REPEAT)
        mInKeyRepeat = false
    }

    fun postDismissKeyPreview(key: Key, delay: Int) {
        removeMessages(MSG_DISMISS_KEY_PREVIEW)
        val msg = obtainMessage(MSG_DISMISS_KEY_PREVIEW, key)
        sendMessageDelayed(msg, delay.toLong())
    }

    fun cancelAllMessages() {
        removeMessages(MSG_KEY_REPEAT)
        removeMessages(MSG_LONGPRESS)
        removeMessages(MSG_LONGPRESS_SHIFT)
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
        removeMessages(MSG_TYPING_STATE)
        removeMessages(MSG_DISMISS_KEY_PREVIEW)
        mInKeyRepeat = false
    }

    private class KeyRepeatData(val tracker: PointerTracker, val repeatCount: Int, val code: Int = 0)

    companion object {
        private const val MSG_KEY_REPEAT = 0
        private const val MSG_LONGPRESS = 1
        private const val MSG_LONGPRESS_SHIFT = 2
        private const val MSG_DOUBLE_TAP_SHIFT_KEY = 3
        private const val MSG_TYPING_STATE = 4
        private const val MSG_DISMISS_KEY_PREVIEW = 5
        private const val DOUBLE_TAP_SHIFT_KEY_DELAY = 500
        private const val TYPING_STATE_TIMEOUT = 5000
    }
}
