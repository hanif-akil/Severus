/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2019 Emmanuel
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

import android.content.res.TypedArray
import android.util.Log
import android.view.MotionEvent
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.keyboard.internal.BogusMoveEventDetector
import rkr.simplekeyboard.inputmethod.keyboard.internal.DrawingProxy
import rkr.simplekeyboard.inputmethod.keyboard.internal.PointerTrackerQueue
import rkr.simplekeyboard.inputmethod.keyboard.internal.TimerProxy
import rkr.simplekeyboard.inputmethod.latin.common.Constants
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils
import rkr.simplekeyboard.inputmethod.latin.define.DebugFlags
import rkr.simplekeyboard.inputmethod.latin.settings.Settings
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues

class PointerTracker private constructor(val mPointerId: Int) : PointerTrackerQueue.Element {

    private var mKeyDetector: KeyDetector = KeyDetector()
    private var mKeyboard: Keyboard? = null
    private val mBogusMoveEventDetector = BogusMoveEventDetector()

    private var mDownCoordinates = CoordinateUtils.newInstance()

    private var mCurrentKey: Key? = null
    private var mKeyX: Int = 0
    private var mKeyY: Int = 0

    private var mLastX: Int = 0
    private var mLastY: Int = 0
    private var mStartX: Int = 0
    private var mStartTime: Long = 0
    private var mCursorMoved: Boolean = false

    private var mKeyboardLayoutHasBeenChanged: Boolean = false
    private var mIsTrackingForActionDisabled: Boolean = false
    private var mMoreKeysPanel: MoreKeysPanel? = null

    var mIsInDraggingFinger: Boolean = false
        internal set
    var mIsInSlidingKeyInput: Boolean = false
        internal set
    private var mCurrentRepeatingKeyCode: Int = Constants.NOT_A_CODE
    private var mIsAllowedDraggingFinger: Boolean = false

    override fun isInDraggingFinger(): Boolean = mIsInDraggingFinger

    override fun isInCursorMove(): Boolean = mCursorMoved

    fun getKey(): Key? = mCurrentKey

    override fun isModifier(): Boolean = mCurrentKey?.isModifier == true

    fun getKeyOn(x: Int, y: Int): Key? = mKeyDetector.detectHitKey(x, y)

    private fun setReleasedKeyGraphics(key: Key?, withAnimation: Boolean) {
        key ?: return
        sDrawingProxy.onKeyReleased(key, withAnimation)
        if (key.isShift) {
            for (shiftKey in mKeyboard!!.mShiftKeys) {
                if (shiftKey !== key) {
                    sDrawingProxy.onKeyReleased(shiftKey, false)
                }
            }
        }
        if (key.isAltCodeWhileTyping) {
            val altCode = key.altCode
            val altKey = mKeyboard!!.getKey(altCode)
            if (altKey != null) {
                sDrawingProxy.onKeyReleased(altKey, false)
            }
            for (k in mKeyboard!!.mAltCodeKeysWhileTyping) {
                if (k !== key && k.altCode == altCode) {
                    sDrawingProxy.onKeyReleased(k, false)
                }
            }
        }
    }

    private fun setPressedKeyGraphics(key: Key?) {
        key ?: return
        val altersCode = key.isAltCodeWhileTyping && sTimerProxy.isTypingState()
        sDrawingProxy.onKeyPressed(key, true)
        if (key.isShift) {
            for (shiftKey in mKeyboard!!.mShiftKeys) {
                if (shiftKey !== key) {
                    sDrawingProxy.onKeyPressed(shiftKey, false)
                }
            }
        }
        if (altersCode) {
            val altCode = key.altCode
            val altKey = mKeyboard!!.getKey(altCode)
            if (altKey != null) {
                sDrawingProxy.onKeyPressed(altKey, false)
            }
            for (k in mKeyboard!!.mAltCodeKeysWhileTyping) {
                if (k !== key && k.altCode == altCode) {
                    sDrawingProxy.onKeyPressed(k, false)
                }
            }
        }
    }

    fun getLastCoordinates(outCoords: IntArray) {
        CoordinateUtils.set(outCoords, mLastX, mLastY)
    }

    private fun onDownKey(x: Int, y: Int): Key {
        CoordinateUtils.set(mDownCoordinates, x, y)
        mBogusMoveEventDetector.onDownKey()
        return onMoveToNewKey(onMoveKeyInternal(x, y), x, y)
    }

    private fun onMoveKeyInternal(x: Int, y: Int): Key {
        mBogusMoveEventDetector.onMoveKey(getDistance(x, y, mLastX, mLastY))
        mLastX = x
        mLastY = y
        return mKeyDetector.detectHitKey(x, y)!!
    }

    private fun onMoveKey(x: Int, y: Int): Key = onMoveKeyInternal(x, y)

    private fun onMoveToNewKey(newKey: Key?, x: Int, y: Int): Key? {
        mCurrentKey = newKey
        mKeyX = x
        mKeyY = y
        return newKey
    }

    fun processMotionEvent(me: MotionEvent, keyDetector: KeyDetector) {
        val action = me.actionMasked
        val eventTime = me.eventTime
        if (action == MotionEvent.ACTION_MOVE) {
            val shouldIgnoreOtherPointers =
                isShowingMoreKeysPanel() && getActivePointerTrackerCount() == 1
            val pointerCount = me.pointerCount
            for (index in 0 until pointerCount) {
                val id = me.getPointerId(index)
                if (shouldIgnoreOtherPointers && id != mPointerId) continue
                val x = me.getX(index).toInt()
                val y = me.getY(index).toInt()
                val tracker = getPointerTracker(id)
                tracker.onMoveEvent(x, y, eventTime)
            }
            return
        }
        val index = me.actionIndex
        val x = me.getX(index).toInt()
        val y = me.getY(index).toInt()
        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(x, y, eventTime, keyDetector)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> onUpEvent(x, y, eventTime)
            MotionEvent.ACTION_CANCEL -> onCancelEvent(x, y, eventTime)
        }
    }

    private fun onDownEvent(x: Int, y: Int, eventTime: Long, keyDetector: KeyDetector) {
        setKeyDetectorInner(keyDetector)
        val deltaT = eventTime
        if (deltaT < sParams.mTouchNoiseThresholdTime) {
            val distance = getDistance(x, y, mLastX, mLastY)
            if (distance < sParams.mTouchNoiseThresholdDistance) {
                if (DEBUG_MODE) Log.w(TAG, "[$mPointerId] onDownEvent: ignore potential noise: time=$deltaT distance=$distance")
                cancelTrackingForAction()
                return
            }
        }

        val key = getKeyOn(x, y)
        mBogusMoveEventDetector.onActualDownEvent(x, y)
        if (key != null && key.isModifier) {
            sPointerTrackerQueue.releaseAllPointers(eventTime)
        }
        sPointerTrackerQueue.add(this)
        onDownEventInternal(x, y)
    }

    internal fun isShowingMoreKeysPanel(): Boolean = mMoreKeysPanel != null

    private fun dismissMoreKeysPanel() {
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel!!.dismissMoreKeysPanel()
            mMoreKeysPanel = null
        }
    }

    private fun onDownEventInternal(x: Int, y: Int) {
        var key = onDownKey(x, y)
        mIsAllowedDraggingFinger = sParams.mKeySelectionByDraggingFinger ||
                (key != null && key.isModifier) ||
                mKeyDetector.alwaysAllowsKeySelectionByDraggingFinger()
        mKeyboardLayoutHasBeenChanged = false
        mIsTrackingForActionDisabled = false
        resetKeySelectionByDraggingFinger()
        if (key != null) {
            if (callListenerOnPressAndCheckKeyboardLayoutChange(key, 0)) {
                key = onDownKey(x, y)
            }
            startRepeatKey(key!!)
            startLongPressTimer(key)
            setPressedKeyGraphics(key)
            mStartX = x
            mStartTime = System.currentTimeMillis()
        }
    }

    private fun startKeySelectionByDraggingFinger(key: Key) {
        if (!mIsInDraggingFinger) {
            mIsInSlidingKeyInput = key.isModifier
        }
        mIsInDraggingFinger = true
    }

    private fun resetKeySelectionByDraggingFinger() {
        mIsInDraggingFinger = false
        mIsInSlidingKeyInput = false
    }

    private fun onMoveEvent(x: Int, y: Int, eventTime: Long) {
        if (mIsTrackingForActionDisabled) return
        if (isShowingMoreKeysPanel()) {
            val translatedX = mMoreKeysPanel!!.translateX(x)
            val translatedY = mMoreKeysPanel!!.translateY(y)
            mMoreKeysPanel!!.onMoveEvent(translatedX, translatedY, mPointerId)
            onMoveKey(x, y)
            return
        }
        onMoveEventInternal(x, y, eventTime)
    }

    private fun processDraggingFingerInToNewKey(newKey: Key?, x: Int, y: Int) {
        var key = newKey
        if (callListenerOnPressAndCheckKeyboardLayoutChange(key!!, 0)) {
            key = onMoveKey(x, y)
        }
        onMoveToNewKey(key, x, y)
        if (mIsTrackingForActionDisabled) return
        startLongPressTimer(key)
        setPressedKeyGraphics(key)
    }

    private fun processDraggingFingerOutFromOldKey(oldKey: Key) {
        setReleasedKeyGraphics(oldKey, true)
        callListenerOnRelease(oldKey, oldKey.code, true)
        startKeySelectionByDraggingFinger(oldKey)
        sTimerProxy.cancelKeyTimersOf(this)
    }

    private fun dragFingerFromOldKeyToNewKey(key: Key?, x: Int, y: Int, eventTime: Long, oldKey: Key) {
        processDraggingFingerOutFromOldKey(oldKey)
        startRepeatKey(key)
        if (mIsAllowedDraggingFinger) {
            processDraggingFingerInToNewKey(key, x, y)
        } else if (getActivePointerTrackerCount() > 1 &&
            !sPointerTrackerQueue.hasModifierKeyOlderThan(this)
        ) {
            if (DEBUG_MODE) Log.w(TAG, "[$mPointerId] onMoveEvent: detected sliding finger while multi touching")
            onUpEvent(x, y, eventTime)
            cancelTrackingForAction()
            setReleasedKeyGraphics(oldKey, true)
        } else {
            cancelTrackingForAction()
            setReleasedKeyGraphics(oldKey, true)
        }
    }

    private fun dragFingerOutFromOldKey(oldKey: Key, x: Int, y: Int) {
        processDraggingFingerOutFromOldKey(oldKey)
        if (mIsAllowedDraggingFinger) {
            onMoveToNewKey(null, x, y)
        } else {
            cancelTrackingForAction()
        }
    }

    private fun onMoveEventInternal(x: Int, y: Int, eventTime: Long) {
        val oldKey = mCurrentKey

        if (oldKey != null && oldKey.code == Constants.CODE_SPACE && Settings.getInstance().getCurrent().mSpaceSwipeEnabled) {
            val steps = (x - mStartX) / sPointerStep
            val swipeIgnoreTime = Settings.getInstance().getCurrent().mKeyLongpressTimeout / MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT
            if (steps != 0 && mStartTime + swipeIgnoreTime < System.currentTimeMillis()) {
                mCursorMoved = true
                mStartX += steps * sPointerStep
                sListener.onMoveCursorPointer(steps)
            }
            return
        }

        if (oldKey != null && oldKey.code == Constants.CODE_DELETE && Settings.getInstance().getCurrent().mDeleteSwipeEnabled) {
            val steps = (x - mStartX) / sPointerStep
            if (steps != 0) {
                sTimerProxy.cancelKeyTimersOf(this)
                mCursorMoved = true
                mStartX += steps * sPointerStep
                sListener.onMoveDeletePointer(steps)
            }
            return
        }

        val newKey = onMoveKey(x, y)
        if (newKey != null) {
            if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, newKey)) {
                dragFingerFromOldKeyToNewKey(newKey, x, y, eventTime, oldKey)
            } else if (oldKey == null) {
                processDraggingFingerInToNewKey(newKey, x, y)
            }
        } else {
            if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, newKey)) {
                dragFingerOutFromOldKey(oldKey, x, y)
            }
        }
    }

    private fun onUpEvent(x: Int, y: Int, eventTime: Long) {
        if (mCurrentKey?.isModifier == true) {
            sPointerTrackerQueue.releaseAllPointersExcept(this, eventTime)
        } else {
            sPointerTrackerQueue.releaseAllPointersOlderThan(this, eventTime)
        }
        onUpEventInternal(x, y)
        sPointerTrackerQueue.remove(this)
    }

    override fun onPhantomUpEvent(eventTime: Long) {
        onUpEventInternal(mLastX, mLastY)
        cancelTrackingForAction()
    }

    private fun onUpEventInternal(x: Int, y: Int) {
        sTimerProxy.cancelKeyTimersOf(this)
        val isInDraggingFinger = mIsInDraggingFinger
        val isInSlidingKeyInput = mIsInSlidingKeyInput
        resetKeySelectionByDraggingFinger()
        val currentKey = mCurrentKey
        mCurrentKey = null
        val currentRepeatingKeyCode = mCurrentRepeatingKeyCode
        mCurrentRepeatingKeyCode = Constants.NOT_A_CODE
        setReleasedKeyGraphics(currentKey, true)

        if (mCursorMoved && currentKey?.code == Constants.CODE_DELETE) {
            sListener.onUpWithDeletePointerActive()
        }
        if (mCursorMoved && currentKey?.code == Constants.CODE_SPACE) {
            sListener.onUpWithSpacePointerActive()
        }

        if (isShowingMoreKeysPanel()) {
            if (!mIsTrackingForActionDisabled) {
                val translatedX = mMoreKeysPanel!!.translateX(x)
                val translatedY = mMoreKeysPanel!!.translateY(y)
                mMoreKeysPanel!!.onUpEvent(translatedX, translatedY, mPointerId)
            }
            dismissMoreKeysPanel()
            return
        }

        if (mCursorMoved) {
            mCursorMoved = false
            return
        }
        if (mIsTrackingForActionDisabled) return
        if (currentKey != null && currentKey.isRepeatable &&
            currentKey.code == currentRepeatingKeyCode && !isInDraggingFinger
        ) return
        detectAndSendKey(currentKey, mKeyX, mKeyY)
        if (isInSlidingKeyInput) {
            callListenerOnFinishSlidingInput()
        }
    }

    override fun cancelTrackingForAction() {
        if (isShowingMoreKeysPanel()) return
        mIsTrackingForActionDisabled = true
    }

    fun onLongPressed() {
        sTimerProxy.cancelLongPressTimersOf(this)
        if (isShowingMoreKeysPanel()) return
        if (mCursorMoved) return
        val key = getKey() ?: return

        val settingsValues = Settings.getInstance().getCurrent()
        if (settingsValues != null && settingsValues.mLongPressCvxEnabled) {
            val code = key.code
            val longPressCode = when (code) {
                'c'.code, 'C'.code -> Constants.CODE_COPY
                'v'.code, 'V'.code -> Constants.CODE_PASTE
                'x'.code, 'X'.code -> Constants.CODE_CUT
                else -> Constants.NOT_A_CODE
            }
            if (longPressCode != Constants.NOT_A_CODE) {
                cancelKeyTracking()
                sListener.onPressKey(longPressCode, 0, true)
                sListener.onCodeInput(longPressCode, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
                sListener.onReleaseKey(longPressCode, false)
                return
            }
        }
        if (key.hasNoPanelAutoMoreKey) {
            cancelKeyTracking()
            val moreKeyCode = key.moreKeys!![0].mCode
            sListener.onPressKey(moreKeyCode, 0, true)
            sListener.onCodeInput(moreKeyCode, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
            sListener.onReleaseKey(moreKeyCode, false)
            return
        }
        val code = key.code
        if (code == Constants.CODE_SPACE || code == Constants.CODE_LANGUAGE_SWITCH) {
            if (sListener.onCustomRequest(Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER)) {
                cancelKeyTracking()
                sListener.onReleaseKey(code, false)
                return
            }
        }

        setReleasedKeyGraphics(key, false)
        val moreKeysPanel = sDrawingProxy.showMoreKeysKeyboard(key, this)
        if (moreKeysPanel == null) return
        val translatedX = moreKeysPanel.translateX(mLastX)
        val translatedY = moreKeysPanel.translateY(mLastY)
        moreKeysPanel.onDownEvent(translatedX, translatedY, mPointerId)
        mMoreKeysPanel = moreKeysPanel
    }

    private fun cancelKeyTracking() {
        resetKeySelectionByDraggingFinger()
        cancelTrackingForAction()
        setReleasedKeyGraphics(mCurrentKey, true)
        sPointerTrackerQueue.remove(this)
    }

    private fun onCancelEvent(x: Int, y: Int, eventTime: Long) {
        cancelAllPointerTrackers()
        sPointerTrackerQueue.releaseAllPointers(eventTime)
        onCancelEventInternal()
    }

    private fun onCancelEventInternal() {
        sTimerProxy.cancelKeyTimersOf(this)
        setReleasedKeyGraphics(mCurrentKey, true)
        resetKeySelectionByDraggingFinger()
        dismissMoreKeysPanel()
    }

    private fun isMajorEnoughMoveToBeOnNewKey(x: Int, y: Int, newKey: Key?): Boolean {
        val curKey = mCurrentKey
        if (newKey === curKey) return false
        if (curKey == null) return true

        val keyHysteresisDistanceSquared = mKeyDetector.getKeyHysteresisDistanceSquared(mIsInSlidingKeyInput)
        val distanceFromKeyEdgeSquared = curKey.squaredDistanceToHitboxEdge(x, y)
        if (distanceFromKeyEdgeSquared >= keyHysteresisDistanceSquared) {
            if (DEBUG_MODE) {
                val distanceToEdgeRatio = Math.sqrt(distanceFromKeyEdgeSquared.toDouble()) /
                        (mKeyboard!!.mMostCommonKeyWidth + mKeyboard!!.mHorizontalGap)
                Log.d(TAG, "[$mPointerId] isMajorEnoughMoveToBeOnNewKey: %.2f key width from key edge".format(distanceToEdgeRatio))
            }
            return true
        }
        if (!mIsAllowedDraggingFinger && mBogusMoveEventDetector.hasTraveledLongDistance(x, y)) {
            if (DEBUG_MODE) {
                val keyDiagonal = Math.hypot(
                    (mKeyboard!!.mMostCommonKeyWidth + mKeyboard!!.mHorizontalGap).toDouble(),
                    (mKeyboard!!.mMostCommonKeyHeight + mKeyboard!!.mVerticalGap).toDouble()
                )
                val lengthFromDownRatio = mBogusMoveEventDetector.getAccumulatedDistanceFromDownKey() / keyDiagonal
                Log.d(TAG, "[$mPointerId] isMajorEnoughMoveToBeOnNewKey: %.2f key diagonal from virtual down point".format(lengthFromDownRatio))
            }
            return true
        }
        return false
    }

    private fun startLongPressTimer(key: Key) {
        sTimerProxy.cancelLongPressShiftKeyTimer()
        if (!key.isLongPressEnabled) return
        if (mIsInDraggingFinger && key.moreKeys == null) return
        val delay = getLongPressTimeout(key.code)
        if (delay <= 0) return
        sTimerProxy.startLongPressTimerOf(this, delay)
    }

    private fun getLongPressTimeout(code: Int): Int {
        if (code == Constants.CODE_SHIFT) return sParams.mLongPressShiftLockTimeout
        val longpressTimeout = Settings.getInstance().getCurrent().mKeyLongpressTimeout
        if (mIsInSlidingKeyInput) {
            return longpressTimeout * MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT
        }
        if (code == Constants.CODE_SPACE) {
            return longpressTimeout * MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT
        }
        return longpressTimeout
    }

    private fun detectAndSendKey(key: Key?, x: Int, y: Int) {
        key ?: return
        val code = key.code
        callListenerOnCodeInput(key, code, x, y, false)
        callListenerOnRelease(key, code, false)
    }

    private fun startRepeatKey(key: Key) {
        if (!key.isRepeatable) return
        if (mIsInDraggingFinger) return
        startKeyRepeatTimer(1)
    }

    fun onKeyRepeat(code: Int, repeatCount: Int) {
        val key = getKey()
        if (key == null || key.code != code) {
            mCurrentRepeatingKeyCode = Constants.NOT_A_CODE
            return
        }
        mCurrentRepeatingKeyCode = code
        val nextRepeatCount = repeatCount + 1
        startKeyRepeatTimer(nextRepeatCount)
        callListenerOnPressAndCheckKeyboardLayoutChange(key, repeatCount)
        callListenerOnCodeInput(key, code, mKeyX, mKeyY, true)
    }

    private fun startKeyRepeatTimer(repeatCount: Int) {
        val delay = if (repeatCount == 1) sParams.mKeyRepeatStartTimeout else sParams.mKeyRepeatInterval
        sTimerProxy.startKeyRepeatTimerOf(this, repeatCount, delay)
    }

    private fun callListenerOnPressAndCheckKeyboardLayoutChange(key: Key, repeatCount: Int): Boolean {
        val ignoreModifierKey = mIsInDraggingFinger && key.isModifier
        if (DEBUG_LISTENER) {
            Log.d(TAG, "[$mPointerId] onPress    : ${if (key == null) "none" else Constants.printableCode(key.code)}${if (ignoreModifierKey) " ignoreModifier" else ""}${if (repeatCount > 0) " repeatCount=$repeatCount" else ""}")
        }
        if (ignoreModifierKey) return false
        sListener.onPressKey(key.code, repeatCount, getActivePointerTrackerCount() == 1)
        val keyboardLayoutHasBeenChanged = mKeyboardLayoutHasBeenChanged
        mKeyboardLayoutHasBeenChanged = false
        sTimerProxy.startTypingStateTimer(key)
        return keyboardLayoutHasBeenChanged
    }

    private fun callListenerOnCodeInput(key: Key, primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
        val ignoreModifierKey = mIsInDraggingFinger && key.isModifier
        val altersCode = key.isAltCodeWhileTyping && sTimerProxy.isTypingState()
        val code = if (altersCode) key.altCode else primaryCode
        if (DEBUG_LISTENER) {
            val output = if (code == Constants.CODE_OUTPUT_TEXT) key.outputText else Constants.printableCode(code)
            Log.d(TAG, "[$mPointerId] onCodeInput: ${"%4d".format(x)} ${"%4d".format(y)} $output${if (ignoreModifierKey) " ignoreModifier" else ""}${if (altersCode) " altersCode" else ""}")
        }
        if (ignoreModifierKey) return
        if (code == Constants.CODE_OUTPUT_TEXT) {
            sListener.onTextInput(key.outputText!!)
        } else if (code != Constants.CODE_UNSPECIFIED) {
            sListener.onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, isKeyRepeat)
        }
    }

    private fun callListenerOnRelease(key: Key, primaryCode: Int, withSliding: Boolean) {
        val ignoreModifierKey = mIsInDraggingFinger && key.isModifier
        if (DEBUG_LISTENER) {
            Log.d(TAG, "[$mPointerId] onRelease  : ${Constants.printableCode(primaryCode)}${if (withSliding) " sliding" else ""}${if (ignoreModifierKey) " ignoreModifier" else ""}")
        }
        if (ignoreModifierKey) return
        sListener.onReleaseKey(primaryCode, withSliding)
    }

    private fun callListenerOnFinishSlidingInput() {
        if (DEBUG_LISTENER) Log.d(TAG, "[$mPointerId] onFinishSlidingInput")
        sListener.onFinishSlidingInput()
    }

    private fun setKeyDetectorInner(keyDetector: KeyDetector) {
        val keyboard = keyDetector.keyboard
        keyboard ?: return
        if (keyDetector == mKeyDetector && keyboard == mKeyboard) return
        mKeyDetector = keyDetector
        mKeyboard = keyboard
        mKeyboardLayoutHasBeenChanged = true
        val keyPaddedWidth = mKeyboard!!.mMostCommonKeyWidth + Math.round(mKeyboard!!.mHorizontalGap)
        val keyPaddedHeight = mKeyboard!!.mMostCommonKeyHeight + Math.round(mKeyboard!!.mVerticalGap)
        mBogusMoveEventDetector.setKeyboardGeometry(keyPaddedWidth, keyPaddedHeight)
    }

    companion object {
        private const val TAG = "PointerTracker"
        private const val DEBUG_EVENT = false
        private const val DEBUG_MOVE_EVENT = false
        private const val DEBUG_LISTENER = false
        private var DEBUG_MODE = DebugFlags.DEBUG_ENABLED || DEBUG_EVENT

        class PointerTrackerParams(mainKeyboardViewAttr: TypedArray) {
            val mKeySelectionByDraggingFinger: Boolean = mainKeyboardViewAttr.getBoolean(
                R.styleable.MainKeyboardView_keySelectionByDraggingFinger, false
            )
            val mTouchNoiseThresholdTime: Int = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_touchNoiseThresholdTime, 0
            )
            val mTouchNoiseThresholdDistance: Int = mainKeyboardViewAttr.getDimensionPixelSize(
                R.styleable.MainKeyboardView_touchNoiseThresholdDistance, 0
            )
            val mKeyRepeatStartTimeout: Int = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_keyRepeatStartTimeout, 0
            )
            val mKeyRepeatInterval: Int = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_keyRepeatInterval, 0
            )
            val mLongPressShiftLockTimeout: Int = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_longPressShiftLockTimeout, 0
            )
        }

        private lateinit var sParams: PointerTrackerParams
        private val sPointerStep: Int = (10.0 * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

        private val sTrackers = ArrayList<PointerTracker>()
        private val sPointerTrackerQueue = PointerTrackerQueue()

        private lateinit var sDrawingProxy: DrawingProxy
        private lateinit var sTimerProxy: TimerProxy
        private var sListener: KeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER

        private const val MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT = 3

        @JvmStatic
        fun init(mainKeyboardViewAttr: TypedArray, timerProxy: TimerProxy, drawingProxy: DrawingProxy) {
            sParams = PointerTrackerParams(mainKeyboardViewAttr)
            val res = mainKeyboardViewAttr.resources
            BogusMoveEventDetector.init(res)
            sTimerProxy = timerProxy
            sDrawingProxy = drawingProxy
        }

        @JvmStatic
        fun getPointerTracker(id: Int): PointerTracker {
            for (i in sTrackers.size..id) {
                sTrackers.add(PointerTracker(i))
            }
            return sTrackers[id]
        }

        @JvmStatic
        fun isAnyInDraggingFinger(): Boolean = sPointerTrackerQueue.isAnyInDraggingFinger()

        @JvmStatic
        fun isAnyInCursorMove(): Boolean = sPointerTrackerQueue.isAnyInCursorMove()

        @JvmStatic
        fun cancelAllPointerTrackers() {
            sPointerTrackerQueue.cancelAllPointerTrackers()
        }

        @JvmStatic
        fun setKeyboardActionListener(listener: KeyboardActionListener) {
            sListener = listener
        }

        @JvmStatic
        fun setKeyDetector(keyDetector: KeyDetector) {
            val keyboard = keyDetector.keyboard ?: return
            for (i in 0 until sTrackers.size) {
                sTrackers[i].setKeyDetectorInner(keyDetector)
            }
        }

        @JvmStatic
        fun setReleasedKeyGraphicsToAllKeys() {
            for (i in 0 until sTrackers.size) {
                val tracker = sTrackers[i]
                tracker.setReleasedKeyGraphics(tracker.getKey(), true)
            }
        }

        @JvmStatic
        fun dismissAllMoreKeysPanels() {
            for (i in 0 until sTrackers.size) {
                sTrackers[i].dismissMoreKeysPanel()
            }
        }

        internal fun getActivePointerTrackerCount(): Int = sPointerTrackerQueue.size

        private fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int =
            Math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toInt()
    }
}
