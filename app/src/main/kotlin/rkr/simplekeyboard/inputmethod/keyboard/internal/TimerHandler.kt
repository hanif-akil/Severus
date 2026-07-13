/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2024 Raimondas Rimkus
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

import android.os.Message
import android.view.ViewConfiguration
import rkr.simplekeyboard.inputmethod.keyboard.Key
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.utils.LeakGuardHandlerWrapper

class TimerHandler(ownerInstance: DrawingProxy, private val mIgnoreAltCodeKeyTimeout: Int) :
    LeakGuardHandlerWrapper<DrawingProxy>(ownerInstance), TimerProxy {

    override fun handleMessage(msg: Message) {
        val drawingProxy = ownerInstance ?: return
        when (msg.what) {
            MSG_REPEAT_KEY -> {
                val tracker = msg.obj as PointerTracker
                tracker.onKeyRepeat(msg.arg1, msg.arg2)
            }
            MSG_LONGPRESS_KEY, MSG_LONGPRESS_SHIFT_KEY -> {
                cancelLongPressTimers()
                val tracker = msg.obj as PointerTracker
                tracker.onLongPressed()
            }
            MSG_DISMISS_KEY_PREVIEW -> {
                drawingProxy.onKeyReleased(msg.obj as Key, false)
            }
        }
    }

    override fun startKeyRepeatTimerOf(tracker: PointerTracker, repeatCount: Int, delay: Int) {
        val key = tracker.getKey() ?: return
        if (delay == 0) return
        sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, key.getCode(), repeatCount, tracker), delay.toLong())
    }

    private fun cancelKeyRepeatTimerOf(tracker: PointerTracker) {
        removeMessages(MSG_REPEAT_KEY, tracker)
    }

    fun cancelKeyRepeatTimers() {
        removeMessages(MSG_REPEAT_KEY)
    }

    fun isInKeyRepeat(): Boolean = hasMessages(MSG_REPEAT_KEY)

    override fun startLongPressTimerOf(tracker: PointerTracker, delay: Int) {
        val key = tracker.getKey() ?: return
        val messageId = if (key.getCode() == Constants.CODE_SHIFT) MSG_LONGPRESS_SHIFT_KEY else MSG_LONGPRESS_KEY
        sendMessageDelayed(obtainMessage(messageId, tracker), delay.toLong())
    }

    override fun cancelLongPressTimersOf(tracker: PointerTracker) {
        removeMessages(MSG_LONGPRESS_KEY, tracker)
        removeMessages(MSG_LONGPRESS_SHIFT_KEY, tracker)
    }

    override fun cancelLongPressShiftKeyTimer() {
        removeMessages(MSG_LONGPRESS_SHIFT_KEY)
    }

    fun cancelLongPressTimers() {
        removeMessages(MSG_LONGPRESS_KEY)
        removeMessages(MSG_LONGPRESS_SHIFT_KEY)
    }

    override fun startTypingStateTimer(typedKey: Key) {
        if (typedKey.isModifier() || typedKey.altCodeWhileTyping()) return
        val isTyping = isTypingState()
        removeMessages(MSG_TYPING_STATE_EXPIRED)
        val drawingProxy = ownerInstance ?: return
        val typedCode = typedKey.getCode()
        if (typedCode == Constants.CODE_SPACE || typedCode == Constants.CODE_ENTER) {
            if (isTyping) drawingProxy.startWhileTypingAnimation(DrawingProxy.FADE_IN)
            return
        }
        sendMessageDelayed(obtainMessage(MSG_TYPING_STATE_EXPIRED), mIgnoreAltCodeKeyTimeout.toLong())
        if (isTyping) return
        drawingProxy.startWhileTypingAnimation(DrawingProxy.FADE_OUT)
    }

    override fun isTypingState(): Boolean = hasMessages(MSG_TYPING_STATE_EXPIRED)

    override fun startDoubleTapShiftKeyTimer() {
        sendMessageDelayed(obtainMessage(MSG_DOUBLE_TAP_SHIFT_KEY), ViewConfiguration.getDoubleTapTimeout().toLong())
    }

    override fun cancelDoubleTapShiftKeyTimer() {
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY)
    }

    override fun isInDoubleTapShiftKeyTimeout(): Boolean = hasMessages(MSG_DOUBLE_TAP_SHIFT_KEY)

    override fun cancelKeyTimersOf(tracker: PointerTracker) {
        cancelKeyRepeatTimerOf(tracker)
        cancelLongPressTimersOf(tracker)
    }

    fun cancelAllKeyTimers() {
        cancelKeyRepeatTimers()
        cancelLongPressTimers()
    }

    fun postDismissKeyPreview(key: Key, delay: Long) {
        sendMessageDelayed(obtainMessage(MSG_DISMISS_KEY_PREVIEW, key), delay)
    }

    fun cancelAllMessages() {
        cancelAllKeyTimers()
        removeMessages(MSG_DISMISS_KEY_PREVIEW)
    }

    companion object {
        private const val MSG_TYPING_STATE_EXPIRED = 0
        private const val MSG_REPEAT_KEY = 1
        private const val MSG_LONGPRESS_KEY = 2
        private const val MSG_LONGPRESS_SHIFT_KEY = 3
        private const val MSG_DOUBLE_TAP_SHIFT_KEY = 4
        private const val MSG_DISMISS_KEY_PREVIEW = 6
    }
}
