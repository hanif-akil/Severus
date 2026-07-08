package rkr.simplekeyboard.inputmethod.keyboard.internal

import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker

interface TimerProxy {
    fun startTypingStateTimer(typedKey: Key)
    fun isTypingState(): Boolean
    fun startKeyRepeatTimerOf(tracker: PointerTracker, repeatCount: Int, delay: Int)
    fun startLongPressTimerOf(tracker: PointerTracker, delay: Int)
    fun cancelLongPressTimersOf(tracker: PointerTracker)
    fun cancelLongPressShiftKeyTimer()
    fun cancelKeyTimersOf(tracker: PointerTracker)
    fun startDoubleTapShiftKeyTimer()
    fun cancelDoubleTapShiftKeyTimer()
    fun isInDoubleTapShiftKeyTimeout(): Boolean
}
